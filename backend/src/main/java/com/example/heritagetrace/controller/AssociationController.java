package com.example.heritagetrace.controller;

import com.example.heritagetrace.dto.request.AssociationBindRequest;
import com.example.heritagetrace.dto.request.AssociationUpdateRequest;
import com.example.heritagetrace.dto.response.ApiResponse;
import com.example.heritagetrace.dto.response.AssociationDTO;
import com.example.heritagetrace.dto.response.AssociationHistoryDTO;
import com.example.heritagetrace.dto.response.TraceabilityResult;
import com.example.heritagetrace.service.AssociationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/associations")
public class AssociationController {
    @Autowired
    private AssociationService associationService;
    
    @PostMapping
    public ApiResponse<AssociationDTO> bind(@Valid @RequestBody AssociationBindRequest request) {
        AssociationDTO association = associationService.bind(request);
        return ApiResponse.success("绑定成功", association);
    }
    
    @PutMapping("/{id}")
    public ApiResponse<AssociationDTO> update(
            @PathVariable Long id,
            @RequestBody AssociationUpdateRequest request) {
        AssociationDTO association = associationService.update(id, request);
        return ApiResponse.success("更新成功", association);
    }
    
    @PutMapping("/{id}/transfer")
    public ApiResponse<AssociationDTO> transfer(
            @PathVariable Long id,
            @RequestBody AssociationUpdateRequest request) {
        AssociationDTO association = associationService.transfer(id, request);
        return ApiResponse.success("移交成功", association);
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        associationService.delete(id);
        return ApiResponse.success("解绑成功", null);
    }
    
    @GetMapping
    public ApiResponse<java.util.List<AssociationDTO>> list(
            @RequestParam(value = "status", required = false) String status) {
        return ApiResponse.success(associationService.list(status));
    }

    @GetMapping("/ledger")
    public ApiResponse<java.util.List<AssociationHistoryDTO>> ledger(
            @RequestParam(value = "toolId", required = false) Long toolId,
            @RequestParam(value = "inheritorId", required = false) Long inheritorId,
            @RequestParam(value = "projectId", required = false) Long projectId) {
        return ApiResponse.success(associationService.getLedger(toolId, inheritorId, projectId));
    }

    @GetMapping("/{id}/history")
    public ApiResponse<java.util.List<AssociationHistoryDTO>> getHistory(@PathVariable Long id) {
        java.util.List<AssociationHistoryDTO> history = associationService.getHistory(id);
        return ApiResponse.success(history);
    }
    
    @GetMapping("/trace/project/{projectId}")
    public ApiResponse<TraceabilityResult> traceByProject(@PathVariable Long projectId) {
        TraceabilityResult result = associationService.traceByProject(projectId);
        return ApiResponse.success(result);
    }
    
    @GetMapping("/trace/inheritor/{inheritorId}")
    public ApiResponse<TraceabilityResult> traceByInheritor(@PathVariable Long inheritorId) {
        TraceabilityResult result = associationService.traceByInheritor(inheritorId);
        return ApiResponse.success(result);
    }
    
    @GetMapping("/trace/tool/{toolId}")
    public ApiResponse<TraceabilityResult> traceByTool(@PathVariable Long toolId) {
        TraceabilityResult result = associationService.traceByTool(toolId);
        return ApiResponse.success(result);
    }
}