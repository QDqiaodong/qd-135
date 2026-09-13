package com.example.heritagetrace.controller;

import com.example.heritagetrace.dto.request.ProjectCreateRequest;
import com.example.heritagetrace.dto.request.ProjectUpdateRequest;
import com.example.heritagetrace.dto.response.ApiResponse;
import com.example.heritagetrace.dto.response.ProjectDTO;
import com.example.heritagetrace.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    @Autowired
    private ProjectService projectService;
    
    @GetMapping
    public ApiResponse<List<ProjectDTO>> getProjects(@RequestParam(required = false) Boolean tree) {
        List<ProjectDTO> projects;
        if (tree != null && tree) {
            projects = projectService.getProjectTree();
        } else {
            projects = projectService.getAllProjects();
        }
        return ApiResponse.success(projects);
    }
    
    @GetMapping("/{id}")
    public ApiResponse<ProjectDTO> getProject(@PathVariable Long id) {
        ProjectDTO project = projectService.getProjectById(id);
        return ApiResponse.success(project);
    }
    
    @PostMapping
    public ApiResponse<ProjectDTO> createProject(@Valid @RequestBody ProjectCreateRequest request) {
        ProjectDTO project = projectService.createProject(request);
        return ApiResponse.success("创建成功", project);
    }
    
    @PutMapping("/{id}")
    public ApiResponse<ProjectDTO> updateProject(
            @PathVariable Long id,
            @RequestBody ProjectUpdateRequest request) {
        ProjectDTO project = projectService.updateProject(id, request);
        return ApiResponse.success("更新成功", project);
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ApiResponse.success("删除成功", null);
    }
}