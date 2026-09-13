package com.example.heritagetrace.dto.response;

import com.example.heritagetrace.entity.AssociationHistory;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssociationHistoryDTO {
    private Long id;
    private Long associationId;
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

    public static AssociationHistoryDTO fromEntity(AssociationHistory entity) {
        AssociationHistoryDTO dto = new AssociationHistoryDTO();
        dto.setId(entity.getId());
        dto.setAssociationId(entity.getAssociationId());
        dto.setActionType(entity.getActionType());
        dto.setOldInheritorId(entity.getOldInheritorId());
        dto.setNewInheritorId(entity.getNewInheritorId());
        dto.setOldProjectId(entity.getOldProjectId());
        dto.setNewProjectId(entity.getNewProjectId());
        dto.setRemark(entity.getRemark());
        dto.setActionTime(entity.getActionTime());
        return dto;
    }
}
