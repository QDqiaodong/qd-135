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
    private String certificateName;
    private String certificateContentType;
    private Long certificateSize;
    private String certificateUploadTime;
    private String createTime;
    private String updateTime;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 列表查询用构造函数：不包含证书二进制内容，避免一次性加载大字段。
     */
    public InheritorDTO(Long id, String name, String title, String specialty, String contact,
                        String certificateName, String certificateContentType, Long certificateSize,
                        java.time.LocalDateTime certificateUploadTime,
                        java.time.LocalDateTime createTime, java.time.LocalDateTime updateTime) {
        this.id = id;
        this.name = name;
        this.title = title;
        this.specialty = specialty;
        this.contact = contact;
        this.certificateName = certificateName;
        this.certificateContentType = certificateContentType;
        this.certificateSize = certificateSize;
        if (certificateUploadTime != null) {
            this.certificateUploadTime = certificateUploadTime.format(FORMATTER);
        }
        if (createTime != null) {
            this.createTime = createTime.format(FORMATTER);
        }
        if (updateTime != null) {
            this.updateTime = updateTime.format(FORMATTER);
        }
    }

    public InheritorDTO() {
    }

    public static InheritorDTO fromEntity(Inheritor inheritor) {
        InheritorDTO dto = new InheritorDTO();
        dto.setId(inheritor.getId());
        dto.setName(inheritor.getName());
        dto.setTitle(inheritor.getTitle());
        dto.setSpecialty(inheritor.getSpecialty());
        dto.setContact(inheritor.getContact());
        dto.setCertificateName(inheritor.getCertificateName());
        dto.setCertificateContentType(inheritor.getCertificateContentType());
        dto.setCertificateSize(inheritor.getCertificateSize());
        if (inheritor.getCertificateUploadTime() != null) {
            dto.setCertificateUploadTime(inheritor.getCertificateUploadTime().format(FORMATTER));
        }
        if (inheritor.getCreateTime() != null) {
            dto.setCreateTime(inheritor.getCreateTime().format(FORMATTER));
        }
        if (inheritor.getUpdateTime() != null) {
            dto.setUpdateTime(inheritor.getUpdateTime().format(FORMATTER));
        }
        return dto;
    }
}
