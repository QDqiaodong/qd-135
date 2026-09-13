package com.example.heritagetrace.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "tools", indexes = {
    @Index(name = "idx_tool_number", columnList = "toolNumber"),
    @Index(name = "idx_craft_type", columnList = "craftType")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tool {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tool_number", unique = true, nullable = false, length = 50)
    private String toolNumber;
    
    @Column(name = "tool_name", nullable = false, length = 100)
    private String toolName;
    
    @Column(name = "craft_type", length = 50)
    private String craftType;
    
    @Column(name = "material", length = 100)
    private String material;
    
    @Column(name = "specification", length = 200)
    private String specification;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
    
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}