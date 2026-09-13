package com.example.heritagetrace.service;

import com.example.heritagetrace.dto.request.ToolCreateRequest;
import com.example.heritagetrace.dto.request.ToolUpdateRequest;
import com.example.heritagetrace.dto.response.ToolDTO;
import com.example.heritagetrace.entity.Tool;
import com.example.heritagetrace.repository.AssociationRepository;
import com.example.heritagetrace.repository.InheritorRepository;
import com.example.heritagetrace.repository.ProjectRepository;
import com.example.heritagetrace.repository.ToolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ToolService {
    @Autowired
    private ToolRepository toolRepository;
    
    @Autowired
    private AssociationRepository associationRepository;
    
    @Autowired
    private InheritorRepository inheritorRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String TOOL_CACHE_KEY_PREFIX = "tools:";
    private static final String TOOL_LIST_CACHE_KEY_PREFIX = "tools:list:";
    private static final long CACHE_EXPIRE_HOURS = 1;
    private static final long LIST_CACHE_EXPIRE_MINUTES = 30;
    
    @Transactional
    public ToolDTO createTool(ToolCreateRequest request) {
        if (toolRepository.existsByToolNumber(request.getToolNumber())) {
            throw new IllegalArgumentException("工具编号已存在");
        }
        
        Tool tool = Tool.builder()
                .toolNumber(request.getToolNumber())
                .toolName(request.getToolName())
                .craftType(request.getCraftType())
                .material(request.getMaterial())
                .specification(request.getSpecification())
                .description(request.getDescription())
                .build();
        
        Tool saved = toolRepository.save(tool);
        clearListCache();
        return ToolDTO.fromEntity(saved);
    }
    
    public ToolDTO getToolById(Long id) {
        String cacheKey = TOOL_CACHE_KEY_PREFIX + id;
        ToolDTO cached = (ToolDTO) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("工具不存在"));
        
        ToolDTO dto = ToolDTO.fromEntity(tool);
        fillAssociationInfo(dto);
        
        redisTemplate.opsForValue().set(cacheKey, dto, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        return dto;
    }
    
    public Page<ToolDTO> getToolList(int page, int size, String keyword) {
        String cacheKey = TOOL_LIST_CACHE_KEY_PREFIX + page + ":" + size + ":" + (keyword != null ? keyword : "");
        @SuppressWarnings("unchecked")
        Page<ToolDTO> cached = (Page<ToolDTO>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Tool> toolPage;
        
        if (keyword != null && !keyword.isEmpty()) {
            toolPage = toolRepository.searchByKeyword(keyword, pageable);
        } else {
            toolPage = toolRepository.findAll(pageable);
        }
        
        Page<ToolDTO> dtoPage = toolPage.map(tool -> {
            ToolDTO dto = ToolDTO.fromEntity(tool);
            fillAssociationInfo(dto);
            return dto;
        });
        
        redisTemplate.opsForValue().set(cacheKey, dtoPage, LIST_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        return dtoPage;
    }
    
    @Transactional
    public ToolDTO updateTool(Long id, ToolUpdateRequest request) {
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("工具不存在"));
        
        if (request.getToolName() != null) {
            tool.setToolName(request.getToolName());
        }
        if (request.getCraftType() != null) {
            tool.setCraftType(request.getCraftType());
        }
        if (request.getMaterial() != null) {
            tool.setMaterial(request.getMaterial());
        }
        if (request.getSpecification() != null) {
            tool.setSpecification(request.getSpecification());
        }
        if (request.getDescription() != null) {
            tool.setDescription(request.getDescription());
        }
        
        Tool updated = toolRepository.save(tool);
        clearCache(id);
        
        ToolDTO dto = ToolDTO.fromEntity(updated);
        fillAssociationInfo(dto);
        return dto;
    }
    
    @Transactional
    public void deleteTool(Long id) {
        if (!toolRepository.existsById(id)) {
            throw new IllegalArgumentException("工具不存在");
        }
        
        associationRepository.findByToolId(id).forEach(association -> {
            association.setStatus("DELETED");
            associationRepository.save(association);
        });
        
        toolRepository.deleteById(id);
        clearCache(id);
    }
    
    public List<ToolDTO> getToolsByIds(List<Long> ids) {
        List<Tool> tools = toolRepository.findByIdIn(ids);
        return tools.stream()
                .map(tool -> {
                    ToolDTO dto = ToolDTO.fromEntity(tool);
                    fillAssociationInfo(dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    private void fillAssociationInfo(ToolDTO dto) {
        List<com.example.heritagetrace.entity.Association> associations = 
                associationRepository.findActiveByToolId(dto.getId());
        
        if (!associations.isEmpty()) {
            com.example.heritagetrace.entity.Association association = associations.get(0);
            
            inheritorRepository.findById(association.getInheritorId())
                    .ifPresent(inheritor -> dto.setInheritorName(inheritor.getName()));
            
            projectRepository.findById(association.getProjectId())
                    .ifPresent(project -> dto.setProjectName(project.getName()));
        }
    }
    
    private void clearCache(Long id) {
        redisTemplate.delete(TOOL_CACHE_KEY_PREFIX + id);
        clearListCache();
    }
    
    private void clearListCache() {
        redisTemplate.keys(TOOL_LIST_CACHE_KEY_PREFIX + "*").forEach(redisTemplate::delete);
    }
}