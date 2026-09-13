package com.example.heritagetrace.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectCreateRequest {
    @NotBlank(message = "项目名称不能为空")
    private String name;
    
    private String category;
    
    private String description;
    
    private Long parentId;
}