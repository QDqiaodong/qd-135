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
    /** 保养到期日，格式 yyyy-MM-dd；为空表示尚未安排保养。是否过期由前端按当天比较，避免列表缓存冻结标记 */
    private String maintenanceDueDate;
    private String inheritorName;
    private String projectName;
    private String createTime;
    private String updateTime;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static ToolDTO fromEntity(Tool tool) {
        ToolDTO dto = new ToolDTO();
        dto.setId(tool.getId());
        dto.setToolNumber(tool.getToolNumber());
        dto.setToolName(tool.getToolName());
        dto.setCraftType(tool.getCraftType());
        dto.setMaterial(tool.getMaterial());
        dto.setSpecification(tool.getSpecification());
        dto.setDescription(tool.getDescription());
        if (tool.getMaintenanceDueDate() != null) {
            dto.setMaintenanceDueDate(tool.getMaintenanceDueDate().format(DATE_FORMATTER));
        }
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
