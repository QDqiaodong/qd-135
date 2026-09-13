package com.example.heritagetrace.controller;

import com.example.heritagetrace.dto.request.ToolCreateRequest;
import com.example.heritagetrace.dto.request.ToolUpdateRequest;
import com.example.heritagetrace.dto.response.ApiResponse;
import com.example.heritagetrace.dto.response.ToolDTO;
import com.example.heritagetrace.service.ToolService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tools")
public class ToolController {
    @Autowired
    private ToolService toolService;
    
    @GetMapping
    public ApiResponse<Page<ToolDTO>> getTools(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        Page<ToolDTO> tools = toolService.getToolList(page, size, keyword);
        return ApiResponse.success(tools);
    }
    
    @GetMapping("/{id}")
    public ApiResponse<ToolDTO> getTool(@PathVariable Long id) {
        ToolDTO tool = toolService.getToolById(id);
        return ApiResponse.success(tool);
    }
    
    @PostMapping
    public ApiResponse<ToolDTO> createTool(@Valid @RequestBody ToolCreateRequest request) {
        ToolDTO tool = toolService.createTool(request);
        return ApiResponse.success("创建成功", tool);
    }
    
    @PutMapping("/{id}")
    public ApiResponse<ToolDTO> updateTool(
            @PathVariable Long id,
            @RequestBody ToolUpdateRequest request) {
        ToolDTO tool = toolService.updateTool(id, request);
        return ApiResponse.success("更新成功", tool);
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTool(@PathVariable Long id) {
        toolService.deleteTool(id);
        return ApiResponse.success("删除成功", null);
    }
}