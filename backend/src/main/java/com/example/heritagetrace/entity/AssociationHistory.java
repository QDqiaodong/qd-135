package com.example.heritagetrace.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "association_history", indexes = {
    @Index(name = "idx_association_id", columnList = "associationId"),
    @Index(name = "idx_action_type", columnList = "actionType"),
    @Index(name = "idx_action_time", columnList = "actionTime")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssociationHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "association_id", nullable = false)
    private Long associationId;
    
    @Column(name = "action_type", nullable = false, length = 20)
    private String actionType;
    
    @Column(name = "old_inheritor_id")
    private Long oldInheritorId;
    
    @Column(name = "new_inheritor_id")
    private Long newInheritorId;
    
    @Column(name = "old_project_id")
    private Long oldProjectId;
    
    @Column(name = "new_project_id")
    private Long newProjectId;
    
    @Column(name = "remark", columnDefinition = "TEXT")
    private String remark;
    
    @Column(name = "action_time")
    private LocalDateTime actionTime;
    
    @PrePersist
    protected void onCreate() {
        if (actionTime == null) {
            actionTime = LocalDateTime.now();
        }
    }
}