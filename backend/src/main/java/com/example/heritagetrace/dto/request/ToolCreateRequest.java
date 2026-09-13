package com.example.heritagetrace.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ToolCreateRequest {
    @NotBlank(message = "工具编号不能为空")
    private String toolNumber;
    
    @NotBlank(message = "工具名称不能为空")
    private String toolName;
    
    private String craftType;
    
    private String material;
    
    private String specification;
    
    private String description;
}