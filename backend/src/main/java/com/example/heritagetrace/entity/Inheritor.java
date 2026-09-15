package com.example.heritagetrace.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "inheritors", indexes = {
    @Index(name = "idx_name", columnList = "name"),
    @Index(name = "idx_title", columnList = "title")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inheritor {
    /** 在册：正常档案，计入看板在册人数，可以挂进在研项目 */
    public static final String STATUS_ACTIVE = "ACTIVE";
    /** 停档：不计入看板在册人数，不能再挂进在研项目，已挂着的标成停档占用 */
    public static final String STATUS_SUSPENDED = "SUSPENDED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;
    
    @Column(name = "title", length = 50)
    private String title;
    
    @Column(name = "specialty", length = 100)
    private String specialty;
    
    @Column(name = "contact", length = 100)
    private String contact;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "certificate_name", length = 255)
    private String certificateName;

    @Column(name = "certificate_content_type", length = 100)
    private String certificateContentType;

    @Column(name = "certificate_size")
    private Long certificateSize;

    @Lob
    @Column(name = "certificate_data", columnDefinition = "LONGBLOB")
    private byte[] certificateData;

    @Column(name = "certificate_upload_time")
    private LocalDateTime certificateUploadTime;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
    
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (status == null) {
            status = STATUS_ACTIVE;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}