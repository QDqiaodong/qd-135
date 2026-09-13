package com.example.heritagetrace.dto.request;

import lombok.Data;

@Data
public class AssociationUpdateRequest {
    private Long inheritorId;
    
    private Long projectId;
    
    private String remark;
}