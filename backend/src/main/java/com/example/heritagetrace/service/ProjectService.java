package com.example.heritagetrace.service;

import com.example.heritagetrace.dto.request.ProjectCreateRequest;
import com.example.heritagetrace.dto.request.ProjectStageUpdateRequest;
import com.example.heritagetrace.dto.request.ProjectUpdateRequest;
import com.example.heritagetrace.dto.response.ProjectDTO;
import com.example.heritagetrace.entity.Project;
import com.example.heritagetrace.repository.AssociationRepository;
import com.example.heritagetrace.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ProjectService {
    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private AssociationRepository associationRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String PROJECT_CACHE_KEY_PREFIX = "projects:";
    private static final String PROJECT_TREE_CACHE_KEY = "projects:tree";
    private static final long CACHE_EXPIRE_HOURS = 1;
    private static final long TREE_CACHE_EXPIRE_HOURS = 2;

    /** 阶段只能顺着走：在研 → 送审 → 结项，不能跳级也不能退回 */
    private static final List<String> STAGE_FLOW = List.of(
            Project.STAGE_IN_PROGRESS,
            Project.STAGE_UNDER_REVIEW,
            Project.STAGE_COMPLETED);
    
    @Transactional
    public ProjectDTO createProject(ProjectCreateRequest request) {
        validateParentId(null, request.getParentId());

        Project project = Project.builder()
                .name(request.getName())
                .category(request.getCategory())
                .description(request.getDescription())
                .parentId(request.getParentId())
                .build();

        Project saved = projectRepository.save(project);
        clearTreeCache();
        return ProjectDTO.fromEntity(saved);
    }
    
    public ProjectDTO getProjectById(Long id) {
        String cacheKey = PROJECT_CACHE_KEY_PREFIX + id;
        ProjectDTO cached = (ProjectDTO) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在"));
        
        ProjectDTO dto = ProjectDTO.fromEntity(project);
        redisTemplate.opsForValue().set(cacheKey, dto, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        return dto;
    }
    
    public List<ProjectDTO> getProjectTree() {
        @SuppressWarnings("unchecked")
        List<ProjectDTO> cached = (List<ProjectDTO>) redisTemplate.opsForValue().get(PROJECT_TREE_CACHE_KEY);
        if (cached != null) {
            return cached;
        }
        
        List<Project> allProjects = projectRepository.findAll();
        
        Map<Long, ProjectDTO> dtoMap = new HashMap<>();
        List<ProjectDTO> rootProjects = new ArrayList<>();
        
        for (Project project : allProjects) {
            ProjectDTO dto = ProjectDTO.fromEntity(project);
            dtoMap.put(project.getId(), dto);
        }
        
        for (Project project : allProjects) {
            ProjectDTO dto = dtoMap.get(project.getId());
            if (project.getParentId() == null) {
                rootProjects.add(dto);
            } else {
                ProjectDTO parent = dtoMap.get(project.getParentId());
                if (parent != null) {
                    parent.getChildren().add(dto);
                }
                // 父级不存在（或父链成环）的节点挂不到任何节点下，
                // 也不属于根节点，因此不会出现在返回的树里。
            }
        }
        
        redisTemplate.opsForValue().set(PROJECT_TREE_CACHE_KEY, rootProjects, TREE_CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        return rootProjects;
    }
    
    public List<ProjectDTO> getAllProjects() {
        List<Project> projects = projectRepository.findAll();
        return projects.stream()
                .map(ProjectDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public ProjectDTO updateProject(Long id, ProjectUpdateRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在"));
        ensureNotLocked(project);

        if (request.getName() != null) {
            project.setName(request.getName());
        }
        if (request.getCategory() != null) {
            project.setCategory(request.getCategory());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        // 表单始终回传 parentId：null 表示解除挂载，非 null 需通过父级校验
        validateParentId(id, request.getParentId());
        project.setParentId(request.getParentId());

        Project updated = projectRepository.save(project);
        clearCache(id);
        return ProjectDTO.fromEntity(updated);
    }

    /**
     * 管理员挂载父子关系：把 projectId 挂到 parentId 下；
     * parentId 为 null 时解除挂载。父级不存在、自引用或会形成环路时拒绝。
     */
    @Transactional
    public ProjectDTO bindParent(Long projectId, Long parentId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在"));
        ensureNotLocked(project);

        validateParentId(projectId, parentId);
        project.setParentId(parentId);

        Project updated = projectRepository.save(project);
        clearCache(projectId);
        return ProjectDTO.fromEntity(updated);
    }

    /**
     * 推进项目阶段。只允许 在研 → 送审 → 结项 逐级推进：
     * 送审要求项目下至少挂着一位传承人和一件工具；
     * 结项要求项目名下的关联已经全部解开。
     * 项目行加悲观写锁，两人同时点结项时只有先到的能成功，
     * 后到的读到最新阶段后会收到“项目已结项”的提示。
     */
    @Transactional
    public ProjectDTO updateStage(Long id, ProjectStageUpdateRequest request) {
        String target = request.getStage() == null ? "" : request.getStage().trim();
        if (!STAGE_FLOW.contains(target)) {
            throw new IllegalArgumentException("未知的项目阶段：" + target);
        }

        Project project = projectRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在"));

        String current = normalizeStage(project.getStage());
        if (Project.STAGE_COMPLETED.equals(current)) {
            throw new IllegalArgumentException("项目已结项，档案已锁定，不能再操作");
        }
        if (current.equals(target)) {
            throw new IllegalArgumentException("项目已处于「" + stageLabel(target) + "」阶段，请勿重复操作");
        }
        if (STAGE_FLOW.indexOf(target) != STAGE_FLOW.indexOf(current) + 1) {
            throw new IllegalArgumentException("阶段只能按「在研 → 送审 → 结项」逐级推进，不能跳级也不能退回");
        }

        if (Project.STAGE_UNDER_REVIEW.equals(target)) {
            ensureReadyForReview(id);
        }
        if (Project.STAGE_COMPLETED.equals(target)) {
            long activeCount = associationRepository.countByProjectIdAndStatus(id, "ACTIVE");
            if (activeCount > 0) {
                throw new IllegalArgumentException("项目名下还有 " + activeCount + " 条有效关联，全部解开后才能结项");
            }
        }

        project.setStage(target);
        Project saved = projectRepository.save(project);
        clearCache(id);
        return ProjectDTO.fromEntity(saved);
    }

    /** 送审门槛：项目下至少挂着一位传承人和一件工具（均为有效关联） */
    private void ensureReadyForReview(Long projectId) {
        boolean hasInheritor = !associationRepository.findInheritorIdsByProjectId(projectId).isEmpty();
        boolean hasTool = !associationRepository.findToolIdsByProjectId(projectId).isEmpty();
        if (!hasInheritor && !hasTool) {
            throw new IllegalArgumentException("送审要求项目下至少挂着一位传承人和一件工具，当前两者都没有");
        }
        if (!hasInheritor) {
            throw new IllegalArgumentException("送审要求项目下至少挂着一位传承人");
        }
        if (!hasTool) {
            throw new IllegalArgumentException("送审要求项目下至少挂着一件工具");
        }
    }

    /** 结项后项目档案锁定：不许再改档案（更新、挂载父级、删除均拦截） */
    private void ensureNotLocked(Project project) {
        if (Project.STAGE_COMPLETED.equals(normalizeStage(project.getStage()))) {
            throw new IllegalArgumentException("项目已结项，档案已锁定，不能再修改");
        }
    }

    /** 历史数据阶段可能为空，一律按在研对待 */
    private String normalizeStage(String stage) {
        return stage == null ? Project.STAGE_IN_PROGRESS : stage;
    }

    private String stageLabel(String stage) {
        switch (stage) {
            case Project.STAGE_UNDER_REVIEW:
                return "送审";
            case Project.STAGE_COMPLETED:
                return "结项";
            default:
                return "在研";
        }
    }
    
    @Transactional
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在"));
        ensureNotLocked(project);

        List<Project> children = projectRepository.findByParentId(id);
        for (Project child : children) {
            child.setParentId(null);
            projectRepository.save(child);
        }

        projectRepository.deleteById(id);
        clearCache(id);
    }
    
    public List<ProjectDTO> getProjectsByIds(List<Long> ids) {
        List<Project> projects = projectRepository.findByIdIn(ids);
        return projects.stream()
                .map(ProjectDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    private void clearCache(Long id) {
        redisTemplate.delete(PROJECT_CACHE_KEY_PREFIX + id);
        clearTreeCache();
    }

    private void clearTreeCache() {
        redisTemplate.delete(PROJECT_TREE_CACHE_KEY);
    }

    /**
     * 校验挂载关系是否合法。
     *
     * @param projectId 待挂载的项目 id（创建场景传 null）
     * @param parentId  目标父级 id，null 表示挂为顶级项目，无需校验
     */
    private void validateParentId(Long projectId, Long parentId) {
        if (parentId == null) {
            return;
        }
        if (projectId != null && parentId.equals(projectId)) {
            throw new IllegalArgumentException("不能将项目自身设为父级项目");
        }
        if (!projectRepository.existsById(parentId)) {
            throw new IllegalArgumentException("所选父级项目不存在");
        }

        // 沿父链向上查找：若当前项目已在新父级的祖先链上，则会形成环路
        Long cursor = parentId;
        Set<Long> visited = new HashSet<>();
        while (cursor != null) {
            if (projectId != null && cursor.equals(projectId)) {
                throw new IllegalArgumentException("不能将项目挂载到其子项目下，这会形成循环层级");
            }
            if (!visited.add(cursor)) {
                // 父链本身存在历史环数据，避免无限循环
                throw new IllegalArgumentException("当前项目层级存在循环，无法挂载");
            }
            Project ancestor = projectRepository.findById(cursor).orElse(null);
            if (ancestor == null) {
                break;
            }
            cursor = ancestor.getParentId();
        }
    }
}