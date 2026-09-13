package com.example.heritagetrace.dto.response;

import com.example.heritagetrace.entity.Tool;
import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
public class ToolDTO {
    private Long id;
    private String toolNumber;
    private String toolName;
    private String craftType;
    private String material;
    private String specification;
    private String description;
    private String inheritorName;
    private String projectName;
    private String createTime;
    private String updateTime;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public static ToolDTO fromEntity(Tool tool) {
        ToolDTO dto = new ToolDTO();
        dto.setId(tool.getId());
        dto.setToolNumber(tool.getToolNumber());
        dto.setToolName(tool.getToolName());
        dto.setCraftType(tool.getCraftType());
        dto.setMaterial(tool.getMaterial());
        dto.setSpecification(tool.getSpecification());
        dto.setDescription(tool.getDescription());
        if (tool.getCreateTime() != null) {
            dto.setCreateTime(tool.getCreateTime().format(FORMATTER));
        }
        if (tool.getUpdateTime() != null) {
            dto.setUpdateTime(tool.getUpdateTime().format(FORMATTER));
        }
        return dto;
    }
    
    public static ToolDTO fromEntity(Tool tool, String inheritorName, String projectName) {
        ToolDTO dto = fromEntity(tool);
        dto.setInheritorName(inheritorName);
        dto.setProjectName(projectName);
        return dto;
    }
}