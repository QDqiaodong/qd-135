package com.example.heritagetrace.dto.request;

import lombok.Data;

@Data
public class ToolUpdateRequest {
    private String toolName;
    
    private String craftType;
    
    private String material;
    
    private String specification;
    
    private String description;
}