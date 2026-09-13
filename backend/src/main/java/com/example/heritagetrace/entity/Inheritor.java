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