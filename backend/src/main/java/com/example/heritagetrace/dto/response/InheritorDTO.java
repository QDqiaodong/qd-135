package com.example.heritagetrace.dto.response;

import com.example.heritagetrace.entity.Inheritor;
import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
public class InheritorDTO {
    private Long id;
    private String name;
    private String title;
    private String specialty;
    private String contact;
    private String createTime;
    private String updateTime;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public static InheritorDTO fromEntity(Inheritor inheritor) {
        InheritorDTO dto = new InheritorDTO();
        dto.setId(inheritor.getId());
        dto.setName(inheritor.getName());
        dto.setTitle(inheritor.getTitle());
        dto.setSpecialty(inheritor.getSpecialty());
        dto.setContact(inheritor.getContact());
        if (inheritor.getCreateTime() != null) {
            dto.setCreateTime(inheritor.getCreateTime().format(FORMATTER));
        }
        if (inheritor.getUpdateTime() != null) {
            dto.setUpdateTime(inheritor.getUpdateTime().format(FORMATTER));
        }
        return dto;
    }
}