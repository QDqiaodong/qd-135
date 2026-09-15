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

    /** 保养到期日，格式 yyyy-MM-dd；为空表示暂不安排保养 */
    private String maintenanceDueDate;
}