package com.example.heritagetrace.dto.response;

import com.example.heritagetrace.entity.Project;
import lombok.Data;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectDTO {
    private Long id;
    private String name;
    private String category;
    private String description;
    private Long parentId;
    private String stage;
    private List<ProjectDTO> children = new ArrayList<>();
    private String createTime;
    private String updateTime;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public static ProjectDTO fromEntity(Project project) {
        ProjectDTO dto = new ProjectDTO();
        dto.setId(project.getId());
        dto.setName(project.getName());
        dto.setCategory(project.getCategory());
        dto.setDescription(project.getDescription());
        dto.setParentId(project.getParentId());
        // 历史数据可能没有阶段，一律按在研兜底
        dto.setStage(project.getStage() == null ? Project.STAGE_IN_PROGRESS : project.getStage());
        if (project.getCreateTime() != null) {
            dto.setCreateTime(project.getCreateTime().format(FORMATTER));
        }
        if (project.getUpdateTime() != null) {
            dto.setUpdateTime(project.getUpdateTime().format(FORMATTER));
        }
        return dto;
    }
    
    public static ProjectDTO fromEntity(Project project, List<ProjectDTO> children) {
        ProjectDTO dto = fromEntity(project);
        dto.setChildren(children);
        return dto;
    }
}