package com.example.heritagetrace.service;

import com.example.heritagetrace.dto.request.ProjectCreateRequest;
import com.example.heritagetrace.dto.request.ProjectUpdateRequest;
import com.example.heritagetrace.dto.response.ProjectDTO;
import com.example.heritagetrace.entity.Project;
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
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String PROJECT_CACHE_KEY_PREFIX = "projects:";
    private static final String PROJECT_TREE_CACHE_KEY = "projects:tree";
    private static final long CACHE_EXPIRE_HOURS = 1;
    private static final long TREE_CACHE_EXPIRE_HOURS = 2;
    
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

        validateParentId(projectId, parentId);
        project.setParentId(parentId);

        Project updated = projectRepository.save(project);
        clearCache(projectId);
        return ProjectDTO.fromEntity(updated);
    }
    
    @Transactional
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new IllegalArgumentException("项目不存在");
        }
        
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