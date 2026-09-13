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
        if (!toolRepository.existsById(request.getToolId())) {
            throw new IllegalArgumentException("工具不存在");
        }
        if (!inheritorRepository.existsById(request.getInheritorId())) {
            throw new IllegalArgumentException("传承人不存在");
        }
        if (!projectRepository.existsById(request.getProjectId())) {
            throw new IllegalArgumentException("非遗项目不存在");
        }
        
        if (associationRepository.existsByToolIdAndInheritorIdAndProjectId(
                request.getToolId(), request.getInheritorId(), request.getProjectId())) {
            throw new IllegalArgumentException("该三方关联已存在");
        }
        
        Association association = Association.builder()
                .toolId(request.getToolId())
                .inheritorId(request.getInheritorId())
                .projectId(request.getProjectId())
                .status("ACTIVE")
                .build();
        
        Association saved = associationRepository.save(association);
        
        AssociationHistory history = AssociationHistory.builder()
                .associationId(saved.getId())
                .actionType("BIND")
                .newInheritorId(request.getInheritorId())
                .newProjectId(request.getProjectId())
                .remark(request.getRemark())
                .build();
        associationHistoryRepository.save(history);
        
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
        String actionType = "UPDATE";
        
        if (request.getInheritorId() != null && !request.getInheritorId().equals(oldInheritorId)) {
            if (!inheritorRepository.existsById(request.getInheritorId())) {
                throw new IllegalArgumentException("传承人不存在");
            }
            association.setInheritorId(request.getInheritorId());
            hasChange = true;
            actionType = "TRANSFER_INHERITOR";
        }
        
        if (request.getProjectId() != null && !request.getProjectId().equals(oldProjectId)) {
            if (!projectRepository.existsById(request.getProjectId())) {
                throw new IllegalArgumentException("非遗项目不存在");
            }
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
        
        if (!"ACTIVE".equals(association.getStatus())) {
            throw new IllegalArgumentException("关联记录已失效");
        }
        
        Long oldInheritorId = association.getInheritorId();
        
        if (request.getInheritorId() == null) {
            throw new IllegalArgumentException("必须指定新传承人");
        }
        
        if (!inheritorRepository.existsById(request.getInheritorId())) {
            throw new IllegalArgumentException("新传承人不存在");
        }
        
        if (request.getInheritorId().equals(oldInheritorId)) {
            throw new IllegalArgumentException("新传承人不能与原传承人相同");
        }
        
        association.setInheritorId(request.getInheritorId());
        Association updated = associationRepository.save(association);
        
        AssociationHistory history = AssociationHistory.builder()
                .associationId(updated.getId())
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
        
        association.setStatus("DELETED");
        associationRepository.save(association);
        
        AssociationHistory history = AssociationHistory.builder()
                .associationId(id)
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
        
        List<InheritorDTO> inheritors = inheritorRepository.findByIdIn(inheritorIds).stream()
                .map(InheritorDTO::fromEntity)
                .collect(Collectors.toList());
        
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
        
        List<InheritorDTO> inheritors = inheritorRepository.findByIdIn(inheritorIds).stream()
                .map(InheritorDTO::fromEntity)
                .collect(Collectors.toList());
        
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
    
    private AssociationDTO buildAssociationDTO(Association association) {
        String toolNumber = toolRepository.findById(association.getToolId())
            .map(Tool::getToolNumber).orElse("");
        String toolName = toolRepository.findById(association.getToolId())
            .map(Tool::getToolName).orElse("");
        String inheritorName = inheritorRepository.findById(association.getInheritorId())
            .map(Inheritor::getName).orElse("");
        String projectName = projectRepository.findById(association.getProjectId())
            .map(Project::getName).orElse("");
        
        return AssociationDTO.fromEntity(association, toolNumber, toolName, inheritorName, projectName);
    }
    
    public List<AssociationHistoryDTO> getHistory(Long associationId) {
        if (!associationRepository.existsById(associationId)) {
            throw new IllegalArgumentException("关联记录不存在");
        }
        
        return associationHistoryRepository.findByAssociationIdOrderByActionTimeDesc(associationId).stream()
                .map(entity -> {
                    AssociationHistoryDTO dto = AssociationHistoryDTO.fromEntity(entity);
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
                })
                .collect(Collectors.toList());
    }
    
    private void clearTraceCache(Long toolId, Long inheritorId, Long projectId) {
        redisTemplate.delete(ASSOCIATION_TRACE_CACHE_KEY_PREFIX + "tool:" + toolId);
        redisTemplate.delete(ASSOCIATION_TRACE_CACHE_KEY_PREFIX + "inheritor:" + inheritorId);
        redisTemplate.delete(ASSOCIATION_TRACE_CACHE_KEY_PREFIX + "project:" + projectId);
    }
}