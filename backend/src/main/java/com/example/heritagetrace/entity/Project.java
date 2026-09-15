package com.example.heritagetrace.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "projects", indexes = {
    @Index(name = "idx_project_name", columnList = "name"),
    @Index(name = "idx_category", columnList = "category"),
    @Index(name = "idx_parent_id", columnList = "parentId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {
    /** 在研：立项后的初始阶段 */
    public static final String STAGE_IN_PROGRESS = "IN_PROGRESS";
    /** 送审：已提交评审，要求名下至少挂着一位传承人和一件工具 */
    public static final String STAGE_UNDER_REVIEW = "UNDER_REVIEW";
    /** 结项：终态，项目档案锁定，不能再挂关联也不能再改档案 */
    public static final String STAGE_COMPLETED = "COMPLETED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "category", length = 50)
    private String category;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "stage", length = 20)
    private String stage;

    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
    
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (stage == null) {
            stage = STAGE_IN_PROGRESS;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}