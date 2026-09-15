package com.example.heritagetrace.dto.response;

import com.example.heritagetrace.entity.AssociationHistory;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class AssociationHistoryDTO {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Long id;
    private Long associationId;
    private Long toolId;
    private String toolNumber;
    private String toolName;
    private String actionType;
    private Long oldInheritorId;
    private String oldInheritorName;
    private Long newInheritorId;
    private String newInheritorName;
    private Long oldProjectId;
    private String oldProjectName;
    private Long newProjectId;
    private String newProjectName;
    private String remark;
    private LocalDateTime actionTime;
    private String actionTimeText;

    public static AssociationHistoryDTO fromEntity(AssociationHistory entity) {
        AssociationHistoryDTO dto = new AssociationHistoryDTO();
        dto.setId(entity.getId());
        dto.setAssociationId(entity.getAssociationId());
        dto.setToolId(entity.getToolId());
        dto.setActionType(entity.getActionType());
        dto.setOldInheritorId(entity.getOldInheritorId());
        dto.setNewInheritorId(entity.getNewInheritorId());
        dto.setOldProjectId(entity.getOldProjectId());
        dto.setNewProjectId(entity.getNewProjectId());
        dto.setRemark(entity.getRemark());
        dto.setActionTime(entity.getActionTime());
        if (entity.getActionTime() != null) {
            dto.setActionTimeText(entity.getActionTime().format(FORMATTER));
        }
        return dto;
    }
}
