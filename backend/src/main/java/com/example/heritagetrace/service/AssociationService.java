package com.example.heritagetrace.service;

import com.example.heritagetrace.dto.request.AssociationBindRequest;
import com.example.heritagetrace.dto.request.AssociationUpdateRequest;
import com.example.heritagetrace.dto.response.AssociationDTO;
import com.example.heritagetrace.dto.response.AssociationHistoryDTO;
import com.example.heritagetrace.dto.response.InheritorDTO;
import com.example.heritagetrace.dto.response.ProjectDTO;
import com.example.heritagetrace.dto.response.ToolDTO;
import com.example.heritagetrace.dto.response.TraceabilityResult;
import com.example.heritagetrace.entity.Association;
import com.example.heritagetrace.entity.AssociationHistory;
import com.example.heritagetrace.entity.Tool;
import com.example.heritagetrace.entity.Inheritor;
import com.example.heritagetrace.entity.Project;
import com.example.heritagetrace.repository.AssociationHistoryRepository;
import com.example.heritagetrace.repository.AssociationRepository;
import com.example.heritagetrace.repository.InheritorRepository;
import com.example.heritagetrace.repository.ProjectRepository;
import com.example.heritagetrace.repository.ToolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssociationService {
    @Autowired
    private AssociationRepository associationRepository;
    
    @Autowired
    private AssociationHistoryRepository associationHistoryRepository;
    
    @Autowired
    private ToolRepository toolRepository;
    
    @Autowired
    private InheritorRepository inheritorRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String ASSOCIATION_TRACE_CACHE_KEY_PREFIX = "associations:";
    
    @Transactional
    public AssociationDTO bind(AssociationBindRequest request) {
        Tool tool = toolRepository.findById(request.getToolId())
                .orElseThrow(() -> new IllegalArgumentException("工具不存在"));
        // 锁住行再核对状态：与停档/启用并发时，以对方先落成的结果为准
        Inheritor inheritor = inheritorRepository.findByIdForUpdate(request.getInheritorId())
                .orElseThrow(() -> new IllegalArgumentException("传承人不存在"));
        ensureInheritorActive(inheritor);
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new IllegalArgumentException("非遗项目不存在"));
        ensureProjectNotCompleted(project);
        ensureCraftMatched(tool, project);

        Association existing = associationRepository
                .findByToolIdAndInheritorIdAndProjectId(
                        request.getToolId(), request.getInheritorId(), request.getProjectId())
                .orElse(null);

        // 工艺对不上的旧记录必须先解开，不允许通过重新挂上“复活”成正常在用
        if (existing != null && Association.STATUS_ACTIVE.equals(existing.getStatus())) {
            throw new IllegalArgumentException("该三方关联已存在");
        }
        if (existing != null && Association.STATUS_MISMATCH.equals(existing.getStatus())) {
            throw new IllegalArgumentException("该关联工艺对不上，仍挂在名单上，请先解开后再按正确工艺重新登记");
        }

        if (existing != null) {
            // 已解开（DELETED）的同一组三方记录，工艺匹配时允许重新挂上
            existing.setStatus(Association.STATUS_ACTIVE);
            existing.setBindTime(java.time.LocalDateTime.now());
            Association reactivated = associationRepository.save(existing);

            AssociationHistory history = AssociationHistory.builder()
                    .associationId(reactivated.getId())
                    .toolId(request.getToolId())
                    .actionType("BIND")
                    .newInheritorId(request.getInheritorId())
                    .newProjectId(request.getProjectId())
                    .remark(request.getRemark())
                    .build();
            associationHistoryRepository.save(history);

            clearTraceCache(request.getToolId(), request.getInheritorId(), request.getProjectId());

            return buildAssociationDTO(reactivated);
        }

        Association association = Association.builder()
                .toolId(request.getToolId())
                .inheritorId(request.getInheritorId())
                .projectId(request.getProjectId())
                .status(Association.STATUS_ACTIVE)
                .build();

        Association saved;
        try {
            saved = associationRepository.save(association);

            AssociationHistory history = AssociationHistory.builder()
                    .associationId(saved.getId())
                    .toolId(request.getToolId())
                    .actionType("BIND")
                    .newInheritorId(request.getInheritorId())
                    .newProjectId(request.getProjectId())
                    .remark(request.getRemark())
                    .build();
            associationHistoryRepository.save(history);
            // 立刻触发刷库，让唯一约束冲突在本事务内抛出，被统一转成 400
            associationRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 两人同一时刻往同一组三方上挂：两边都不能留下，事务整体回滚
            throw new IllegalArgumentException("该三方关联正由其他用户同时挂上，请刷新名单后确认，本次未保留记录");
        }

        clearTraceCache(request.getToolId(), request.getInheritorId(), request.getProjectId());

        return buildAssociationDTO(saved);
    }
    
    @Transactional
    public AssociationDTO update(Long id, AssociationUpdateRequest request) {
        Association association = associationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("关联记录不存在"));

        Long oldInheritorId = association.getInheritorId();
        Long oldProjectId = association.getProjectId();

        boolean hasChange = false;
        boolean inheritorChanging = false;
        String actionType = "UPDATE";

        if (request.getInheritorId() != null && !request.getInheritorId().equals(oldInheritorId)) {
            Inheritor target = inheritorRepository.findByIdForUpdate(request.getInheritorId())
                    .orElseThrow(() -> new IllegalArgumentException("传承人不存在"));
            ensureInheritorActive(target);
            association.setInheritorId(request.getInheritorId());
            hasChange = true;
            inheritorChanging = true;
            actionType = "TRANSFER_INHERITOR";
        }

        if (request.getProjectId() != null && !request.getProjectId().equals(oldProjectId)) {
            Project targetProject = projectRepository.findById(request.getProjectId())
                    .orElseThrow(() -> new IllegalArgumentException("非遗项目不存在"));
            ensureProjectNotCompleted(targetProject);
            if (!inheritorChanging) {
                // 持有人不变、只换项目，等于把同一个人挂进另一个在研项目：
                // 停档占用的关联不许这么动，只能先解开
                Inheritor holder = inheritorRepository.findByIdForUpdate(oldInheritorId).orElse(null);
                if (holder != null && Inheritor.STATUS_SUSPENDED.equals(holder.getStatus())) {
                    throw new IllegalArgumentException("传承人「" + holder.getName()
                            + "」已停档，停档占用的关联请先解开，不能换挂到其他在研项目");
                }
            }
            Tool boundTool = toolRepository.findById(association.getToolId())
                    .orElseThrow(() -> new IllegalArgumentException("关联工具不存在"));
            ensureCraftMatched(boundTool, targetProject);
            association.setProjectId(request.getProjectId());
            hasChange = true;
            if ("TRANSFER_INHERITOR".equals(actionType)) {
                actionType = "TRANSFER_BOTH";
            } else {
                actionType = "TRANSFER_PROJECT";
            }
        }
        
        if (!hasChange) {
            throw new IllegalArgumentException("未检测到变更");
        }
        
        Association updated = associationRepository.save(association);
        
        AssociationHistory history = AssociationHistory.builder()
                .associationId(updated.getId())
                .toolId(association.getToolId())
                .actionType(actionType)
                .oldInheritorId(oldInheritorId)
                .newInheritorId(association.getInheritorId())
                .oldProjectId(oldProjectId)
                .newProjectId(association.getProjectId())
                .remark(request.getRemark())
                .build();
        associationHistoryRepository.save(history);

        clearTraceCache(association.getToolId(), oldInheritorId, oldProjectId);
        clearTraceCache(association.getToolId(), association.getInheritorId(), association.getProjectId());

        return buildAssociationDTO(updated);
    }

    @Transactional
    public AssociationDTO transfer(Long id, AssociationUpdateRequest request) {
        Association association = associationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("关联记录不存在"));
        
        if (!Association.STATUS_ACTIVE.equals(association.getStatus())) {
            throw new IllegalArgumentException("关联记录已失效或工艺对不上，不能移交");
        }
        
        Long oldInheritorId = association.getInheritorId();
        
        if (request.getInheritorId() == null) {
            throw new IllegalArgumentException("必须指定新传承人");
        }

        Inheritor target = inheritorRepository.findByIdForUpdate(request.getInheritorId())
                .orElseThrow(() -> new IllegalArgumentException("新传承人不存在"));
        ensureInheritorActive(target);

        if (request.getInheritorId().equals(oldInheritorId)) {
            throw new IllegalArgumentException("新传承人不能与原传承人相同");
        }
        
        association.setInheritorId(request.getInheritorId());
        Association updated = associationRepository.save(association);
        
        AssociationHistory history = AssociationHistory.builder()
                .associationId(updated.getId())
                .toolId(association.getToolId())
                .actionType("TRANSFER")
                .oldInheritorId(oldInheritorId)
                .newInheritorId(request.getInheritorId())
                .remark(request.getRemark())
                .build();
        associationHistoryRepository.save(history);
        
        clearTraceCache(association.getToolId(), oldInheritorId, association.getProjectId());
        clearTraceCache(association.getToolId(), request.getInheritorId(), association.getProjectId());
        
        return buildAssociationDTO(updated);
    }
    
    @Transactional
    public void delete(Long id) {
        Association association = associationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("关联记录不存在"));

        if (Association.STATUS_DELETED.equals(association.getStatus())) {
            throw new IllegalArgumentException("该关联已解绑，无需重复操作");
        }

        association.setStatus(Association.STATUS_DELETED);
        associationRepository.save(association);

        AssociationHistory history = AssociationHistory.builder()
                .associationId(id)
                .toolId(association.getToolId())
                .actionType("UNBIND")
                .oldInheritorId(association.getInheritorId())
                .oldProjectId(association.getProjectId())
                .build();
        associationHistoryRepository.save(history);

        clearTraceCache(association.getToolId(), association.getInheritorId(), association.getProjectId());
    }
    
    public TraceabilityResult traceByProject(Long projectId) {
        String cacheKey = ASSOCIATION_TRACE_CACHE_KEY_PREFIX + "project:" + projectId;
        @SuppressWarnings("unchecked")
        TraceabilityResult cached = (TraceabilityResult) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        ProjectDTO project = ProjectDTO.fromEntity(projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在")));
        
        List<Long> toolIds = associationRepository.findToolIdsByProjectId(projectId);
        List<Long> inheritorIds = associationRepository.findInheritorIdsByProjectId(projectId);
        List<Association> associations = associationRepository.findActiveByProjectId(projectId);
        
        List<ToolDTO> tools = toolRepository.findByIdIn(toolIds).stream()
                .map(tool -> {
                    ToolDTO dto = ToolDTO.fromEntity(tool);
                    dto.setProjectName(project.getName());
                    return dto;
                })
                .collect(Collectors.toList());
        
        List<InheritorDTO> inheritors = inheritorRepository.findDtoByIdIn(inheritorIds);

        List<AssociationDTO> associationDTOs = associations.stream()
                .map(this::buildAssociationDTO)
                .collect(Collectors.toList());

        TraceabilityResult result = new TraceabilityResult();
        result.setMainEntity(new TraceabilityResult.MainEntity(projectId, project.getName(), "project"));
        result.setAssociatedTools(tools);
        result.setAssociatedInheritors(inheritors);
        result.getAssociatedProjects().add(project);
        result.setAssociations(associationDTOs);
        
        redisTemplate.opsForValue().set(cacheKey, result, 30, java.util.concurrent.TimeUnit.MINUTES);
        return result;
    }
    
    public TraceabilityResult traceByInheritor(Long inheritorId) {
        String cacheKey = ASSOCIATION_TRACE_CACHE_KEY_PREFIX + "inheritor:" + inheritorId;
        @SuppressWarnings("unchecked")
        TraceabilityResult cached = (TraceabilityResult) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        InheritorDTO inheritor = InheritorDTO.fromEntity(inheritorRepository.findById(inheritorId)
                .orElseThrow(() -> new IllegalArgumentException("传承人不存在")));
        
        List<Long> toolIds = associationRepository.findToolIdsByInheritorId(inheritorId);
        List<Long> projectIds = associationRepository.findProjectIdsByInheritorId(inheritorId);
        List<Association> associations = associationRepository.findActiveByInheritorId(inheritorId);
        
        List<ToolDTO> tools = toolRepository.findByIdIn(toolIds).stream()
                .map(tool -> {
                    ToolDTO dto = ToolDTO.fromEntity(tool);
                    dto.setInheritorName(inheritor.getName());
                    return dto;
                })
                .collect(Collectors.toList());
        
        List<ProjectDTO> projects = projectRepository.findByIdIn(projectIds).stream()
                .map(ProjectDTO::fromEntity)
                .collect(Collectors.toList());
        
        List<AssociationDTO> associationDTOs = associations.stream()
                .map(this::buildAssociationDTO)
                .collect(Collectors.toList());
        
        TraceabilityResult result = new TraceabilityResult();
        result.setMainEntity(new TraceabilityResult.MainEntity(inheritorId, inheritor.getName(), "inheritor"));
        result.setAssociatedTools(tools);
        result.getAssociatedInheritors().add(inheritor);
        result.setAssociatedProjects(projects);
        result.setAssociations(associationDTOs);
        
        redisTemplate.opsForValue().set(cacheKey, result, 30, java.util.concurrent.TimeUnit.MINUTES);
        return result;
    }
    
    public TraceabilityResult traceByTool(Long toolId) {
        String cacheKey = ASSOCIATION_TRACE_CACHE_KEY_PREFIX + "tool:" + toolId;
        @SuppressWarnings("unchecked")
        TraceabilityResult cached = (TraceabilityResult) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        ToolDTO tool = ToolDTO.fromEntity(toolRepository.findById(toolId)
                .orElseThrow(() -> new IllegalArgumentException("工具不存在")));
        
        List<Long> inheritorIds = associationRepository.findInheritorIdsByToolId(toolId);
        List<Long> projectIds = associationRepository.findProjectIdsByToolId(toolId);
        List<Association> associations = associationRepository.findActiveByToolId(toolId);
        
        List<InheritorDTO> inheritors = inheritorRepository.findDtoByIdIn(inheritorIds);
        
        List<ProjectDTO> projects = projectRepository.findByIdIn(projectIds).stream()
                .map(ProjectDTO::fromEntity)
                .collect(Collectors.toList());
        
        List<AssociationDTO> associationDTOs = associations.stream()
                .map(this::buildAssociationDTO)
                .collect(Collectors.toList());
        
        TraceabilityResult result = new TraceabilityResult();
        result.setMainEntity(new TraceabilityResult.MainEntity(toolId, tool.getToolName(), "tool"));
        result.getAssociatedTools().add(tool);
        result.setAssociatedInheritors(inheritors);
        result.setAssociatedProjects(projects);
        result.setAssociations(associationDTOs);
        
        redisTemplate.opsForValue().set(cacheKey, result, 30, java.util.concurrent.TimeUnit.MINUTES);
        return result;
    }
    
    /** 结项后的项目不许再挂新的关联 */
    private void ensureProjectNotCompleted(Project project) {
        if (Project.STAGE_COMPLETED.equals(project.getStage())) {
            throw new IllegalArgumentException("项目已结项，不能再挂新的关联");
        }
    }

    /** 停档的人不许再挂进在研项目：新挂、更换、移交都先过这道 */
    private void ensureInheritorActive(Inheritor inheritor) {
        if (Inheritor.STATUS_SUSPENDED.equals(inheritor.getStatus())) {
            throw new IllegalArgumentException("传承人「" + inheritor.getName() + "」已停档，不能再挂进在研项目");
        }
    }

    /** 挂上/换项目时，工具工艺必须与项目分类一路，否则给出具体对不上的提示 */
    private void ensureCraftMatched(Tool tool, Project project) {
        if (!CraftMatcher.isCraftMatched(tool.getCraftType(), project.getCategory())) {
            throw new IllegalArgumentException(CraftMatcher.mismatchMessage(tool.getCraftType(), project.getCategory()));
        }
    }

    /**
     * 工具档案变更后对账：其名下仍挂着（非 DELETED）的关联，
     * 按最新工艺重新判定 ACTIVE / MISMATCH。
     */
    @Transactional
    public void reconcileByTool(Long toolId) {
        Tool tool = toolRepository.findById(toolId).orElse(null);
        for (Association association : associationRepository.findByToolId(toolId)) {
            if (Association.STATUS_DELETED.equals(association.getStatus())) {
                continue;
            }
            Project project = projectRepository.findById(association.getProjectId()).orElse(null);
            boolean matched = tool != null && project != null
                    && CraftMatcher.isCraftMatched(tool.getCraftType(), project.getCategory());
            applyReconciledStatus(association, matched);
        }
    }

    /**
     * 项目档案变更后对账：挂在该项目下（非 DELETED）的关联，
     * 按最新分类重新判定 ACTIVE / MISMATCH。
     */
    @Transactional
    public void reconcileByProject(Long projectId) {
        Project project = projectRepository.findById(projectId).orElse(null);
        for (Association association : associationRepository.findByProjectId(projectId)) {
            if (Association.STATUS_DELETED.equals(association.getStatus())) {
                continue;
            }
            Tool tool = toolRepository.findById(association.getToolId()).orElse(null);
            boolean matched = tool != null && project != null
                    && CraftMatcher.isCraftMatched(tool.getCraftType(), project.getCategory());
            applyReconciledStatus(association, matched);
        }
    }

    private void applyReconciledStatus(Association association, boolean matched) {
        String target = matched ? Association.STATUS_ACTIVE : Association.STATUS_MISMATCH;
        if (target.equals(association.getStatus())) {
            return;
        }
        association.setStatus(target);
        associationRepository.save(association);
        clearTraceCache(association.getToolId(), association.getInheritorId(), association.getProjectId());
    }

    private AssociationDTO buildAssociationDTO(Association association) {
        Tool tool = toolRepository.findById(association.getToolId()).orElse(null);
        Inheritor inheritor = inheritorRepository.findById(association.getInheritorId()).orElse(null);
        Project project = projectRepository.findById(association.getProjectId()).orElse(null);

        String toolNumber = tool != null ? tool.getToolNumber() : "";
        String toolName = tool != null ? tool.getToolName() : "";
        String inheritorName = inheritor != null ? inheritor.getName() : "";
        String projectName = project != null ? project.getName() : "";

        AssociationDTO dto = AssociationDTO.fromEntity(association, toolNumber, toolName, inheritorName, projectName);
        if (tool != null) {
            dto.setToolCraftType(tool.getCraftType());
        }
        if (project != null) {
            dto.setProjectCategory(project.getCategory());
        }
        if (inheritor != null) {
            // 停档占用跟着传承人当前状态走：停档即标出，重新启用即消失
            dto.setInheritorStatus(inheritor.getStatus());
            dto.setInheritorSuspended(Inheritor.STATUS_SUSPENDED.equals(inheritor.getStatus()));
        }
        boolean matched = tool != null && project != null
                && CraftMatcher.isCraftMatched(tool.getCraftType(), project.getCategory());
        dto.setCraftMatched(matched);
        return dto;
    }
    
    public List<AssociationHistoryDTO> getHistory(Long associationId) {
        if (!associationRepository.existsById(associationId)) {
            throw new IllegalArgumentException("关联记录不存在");
        }

        return associationHistoryRepository.findByAssociationIdOrderByActionTimeDesc(associationId).stream()
                .map(this::buildHistoryDTO)
                .collect(Collectors.toList());
    }

    public List<AssociationHistoryDTO> getLedger(Long toolId, Long inheritorId, Long projectId) {
        return associationHistoryRepository.findLedger(toolId, inheritorId, projectId).stream()
                .map(this::buildHistoryDTO)
                .collect(Collectors.toList());
    }

    private AssociationHistoryDTO buildHistoryDTO(AssociationHistory entity) {
        AssociationHistoryDTO dto = AssociationHistoryDTO.fromEntity(entity);

        Long resolvedToolId = entity.getToolId();
        if (resolvedToolId == null) {
            resolvedToolId = associationRepository.findById(entity.getAssociationId())
                    .map(Association::getToolId).orElse(null);
            dto.setToolId(resolvedToolId);
        }
        if (resolvedToolId != null) {
            toolRepository.findById(resolvedToolId).ifPresent(tool -> {
                dto.setToolNumber(tool.getToolNumber());
                dto.setToolName(tool.getToolName());
            });
        }

        dto.setOldInheritorName(entity.getOldInheritorId() != null
                ? inheritorRepository.findById(entity.getOldInheritorId())
                        .map(Inheritor::getName).orElse("")
                : "");
        dto.setNewInheritorName(entity.getNewInheritorId() != null
                ? inheritorRepository.findById(entity.getNewInheritorId())
                        .map(Inheritor::getName).orElse("")
                : "");
        dto.setOldProjectName(entity.getOldProjectId() != null
                ? projectRepository.findById(entity.getOldProjectId())
                        .map(Project::getName).orElse("")
                : "");
        dto.setNewProjectName(entity.getNewProjectId() != null
                ? projectRepository.findById(entity.getNewProjectId())
                        .map(Project::getName).orElse("")
                : "");
        return dto;
    }

    public List<AssociationDTO> list(String status) {
        String normalized = (status == null || status.isBlank()) ? null : status.trim();
        List<Association> associations;
        if (normalized == null) {
            associations = associationRepository.findAll();
        } else if ("OPEN".equals(normalized)) {
            // 名单视角：仍挂着没解开的，正常在用与工艺对不上的都要列出
            associations = associationRepository.findOpen();
        } else {
            associations = associationRepository.findByStatus(normalized);
        }
        return associations.stream()
                .sorted((a, b) -> {
                    java.time.LocalDateTime ta = a.getBindTime();
                    java.time.LocalDateTime tb = b.getBindTime();
                    if (ta == null && tb == null) return 0;
                    if (ta == null) return 1;
                    if (tb == null) return -1;
                    return tb.compareTo(ta);
                })
                .map(this::buildAssociationDTO)
                .collect(Collectors.toList());
    }
    
    private void clearTraceCache(Long toolId, Long inheritorId, Long projectId) {
        redisTemplate.delete(ASSOCIATION_TRACE_CACHE_KEY_PREFIX + "tool:" + toolId);
        redisTemplate.delete(ASSOCIATION_TRACE_CACHE_KEY_PREFIX + "inheritor:" + inheritorId);
        redisTemplate.delete(ASSOCIATION_TRACE_CACHE_KEY_PREFIX + "project:" + projectId);
    }
}