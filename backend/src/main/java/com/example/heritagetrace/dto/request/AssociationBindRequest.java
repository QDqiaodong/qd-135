package com.example.heritagetrace.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssociationBindRequest {
    @NotNull(message = "工具ID不能为空")
    private Long toolId;
    
    @NotNull(message = "传承人ID不能为空")
    private Long inheritorId;
    
    @NotNull(message = "非遗项目ID不能为空")
    private Long projectId;
    
    private String remark;
}