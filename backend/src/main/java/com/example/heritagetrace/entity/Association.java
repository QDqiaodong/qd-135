package com.example.heritagetrace.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "associations", indexes = {
    @Index(name = "idx_tool_id", columnList = "toolId"),
    @Index(name = "idx_inheritor_id", columnList = "inheritorId"),
    @Index(name = "idx_project_id", columnList = "projectId")
}, uniqueConstraints = {
    @UniqueConstraint(name = "idx_unique_association", columnNames = {"toolId", "inheritorId", "projectId"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Association {
    /** 正常在用：工具工艺与项目分类一致 */
    public static final String STATUS_ACTIVE = "ACTIVE";
    /** 工艺对不上：挂错了，在名单上标出，解开前不算正常在用 */
    public static final String STATUS_MISMATCH = "MISMATCH";
    /** 已解开（软删除），仍保留在挂解流水中 */
    public static final String STATUS_DELETED = "DELETED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tool_id", nullable = false)
    private Long toolId;
    
    @Column(name = "inheritor_id", nullable = false)
    private Long inheritorId;
    
    @Column(name = "project_id", nullable = false)
    private Long projectId;
    
    @Column(name = "bind_time")
    private LocalDateTime bindTime;
    
    @Column(name = "status", length = 20)
    private String status;
    
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
    
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (bindTime == null) {
            bindTime = LocalDateTime.now();
        }
        if (status == null) {
            status = STATUS_ACTIVE;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}