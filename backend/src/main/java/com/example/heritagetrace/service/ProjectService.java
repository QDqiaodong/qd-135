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
import java.util.List;
import java.util.Map;
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
        if (request.getParentId() != null) {
            project.setParentId(request.getParentId());
        }
        
        Project updated = projectRepository.save(project);
        clearCache(id);
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
}