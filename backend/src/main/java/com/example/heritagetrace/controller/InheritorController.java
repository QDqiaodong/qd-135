package com.example.heritagetrace.controller;

import com.example.heritagetrace.dto.request.InheritorCreateRequest;
import com.example.heritagetrace.dto.request.InheritorUpdateRequest;
import com.example.heritagetrace.dto.response.ApiResponse;
import com.example.heritagetrace.dto.response.InheritorDTO;
import com.example.heritagetrace.service.InheritorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inheritors")
public class InheritorController {
    @Autowired
    private InheritorService inheritorService;
    
    @GetMapping
    public ApiResponse<Page<InheritorDTO>> getInheritors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        Page<InheritorDTO> inheritors = inheritorService.getInheritorList(page, size, keyword);
        return ApiResponse.success(inheritors);
    }
    
    @GetMapping("/{id}")
    public ApiResponse<InheritorDTO> getInheritor(@PathVariable Long id) {
        InheritorDTO inheritor = inheritorService.getInheritorById(id);
        return ApiResponse.success(inheritor);
    }
    
    @PostMapping
    public ApiResponse<InheritorDTO> createInheritor(@Valid @RequestBody InheritorCreateRequest request) {
        InheritorDTO inheritor = inheritorService.createInheritor(request);
        return ApiResponse.success("创建成功", inheritor);
    }
    
    @PutMapping("/{id}")
    public ApiResponse<InheritorDTO> updateInheritor(
            @PathVariable Long id,
            @RequestBody InheritorUpdateRequest request) {
        InheritorDTO inheritor = inheritorService.updateInheritor(id, request);
        return ApiResponse.success("更新成功", inheritor);
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteInheritor(@PathVariable Long id) {
        inheritorService.deleteInheritor(id);
        return ApiResponse.success("删除成功", null);
    }
}