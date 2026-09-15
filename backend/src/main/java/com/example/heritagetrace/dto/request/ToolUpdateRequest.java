package com.example.heritagetrace.dto.request;

import lombok.Data;

@Data
public class ToolUpdateRequest {
    private String toolName;
    
    private String craftType;
    
    private String material;
    
    private String specification;

    private String description;

    /**
     * 保养到期日，格式 yyyy-MM-dd。
     * null 表示不修改；空串表示清空日期。
     */
    private String maintenanceDueDate;
}