package com.example.heritagetrace.dto.request;

import lombok.Data;

@Data
public class ProjectUpdateRequest {
    private String name;
    
    private String category;
    
    private String description;
    
    private Long parentId;
}