package com.example.heritagetrace.dto.response;

import com.example.heritagetrace.entity.Association;
import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
public class AssociationDTO {
    private Long id;
    private Long toolId;
    private String toolNumber;
    private String toolName;
    private Long inheritorId;
    private String inheritorName;
    private Long projectId;
    private String projectName;
    private String bindTime;
    private String status;
    /** 工具的适用工艺，用于名单上说明对不上 */
    private String toolCraftType;
    /** 项目的分类，用于名单上说明对不上 */
    private String projectCategory;
    /** 工艺与项目分类是否一路（实时比对得出） */
    private boolean craftMatched;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public static AssociationDTO fromEntity(Association association) {
        AssociationDTO dto = new AssociationDTO();
        dto.setId(association.getId());
        dto.setToolId(association.getToolId());
        dto.setInheritorId(association.getInheritorId());
        dto.setProjectId(association.getProjectId());
        if (association.getBindTime() != null) {
            dto.setBindTime(association.getBindTime().format(FORMATTER));
        }
        dto.setStatus(association.getStatus());
        return dto;
    }
    
    public static AssociationDTO fromEntity(Association association, String toolNumber, String toolName, 
                                           String inheritorName, String projectName) {
        AssociationDTO dto = fromEntity(association);
        dto.setToolNumber(toolNumber);
        dto.setToolName(toolName);
        dto.setInheritorName(inheritorName);
        dto.setProjectName(projectName);
        return dto;
    }
}