package com.example.heritagetrace.repository;

import com.example.heritagetrace.entity.Association;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssociationRepository extends JpaRepository<Association, Long> {
    List<Association> findByToolId(Long toolId);
    
    List<Association> findByInheritorId(Long inheritorId);
    
    List<Association> findByProjectId(Long projectId);
    
    Optional<Association> findByToolIdAndInheritorIdAndProjectId(Long toolId, Long inheritorId, Long projectId);
    
    boolean existsByToolIdAndInheritorIdAndProjectId(Long toolId, Long inheritorId, Long projectId);
    
    @Query("SELECT a FROM Association a WHERE a.status = 'ACTIVE' AND a.toolId = :toolId")
    List<Association> findActiveByToolId(@Param("toolId") Long toolId);
    
    @Query("SELECT a FROM Association a WHERE a.status = 'ACTIVE' AND a.inheritorId = :inheritorId")
    List<Association> findActiveByInheritorId(@Param("inheritorId") Long inheritorId);
    
    @Query("SELECT a FROM Association a WHERE a.status = 'ACTIVE' AND a.projectId = :projectId")
    List<Association> findActiveByProjectId(@Param("projectId") Long projectId);
    
    @Query("SELECT DISTINCT a.toolId FROM Association a WHERE a.inheritorId = :inheritorId AND a.status = 'ACTIVE'")
    List<Long> findToolIdsByInheritorId(@Param("inheritorId") Long inheritorId);
    
    @Query("SELECT DISTINCT a.toolId FROM Association a WHERE a.projectId = :projectId AND a.status = 'ACTIVE'")
    List<Long> findToolIdsByProjectId(@Param("projectId") Long projectId);
    
    @Query("SELECT DISTINCT a.inheritorId FROM Association a WHERE a.projectId = :projectId AND a.status = 'ACTIVE'")
    List<Long> findInheritorIdsByProjectId(@Param("projectId") Long projectId);
    
    @Query("SELECT DISTINCT a.inheritorId FROM Association a WHERE a.toolId = :toolId AND a.status = 'ACTIVE'")
    List<Long> findInheritorIdsByToolId(@Param("toolId") Long toolId);
    
    @Query("SELECT DISTINCT a.projectId FROM Association a WHERE a.inheritorId = :inheritorId AND a.status = 'ACTIVE'")
    List<Long> findProjectIdsByInheritorId(@Param("inheritorId") Long inheritorId);
    
    @Query("SELECT DISTINCT a.projectId FROM Association a WHERE a.toolId = :toolId AND a.status = 'ACTIVE'")
    List<Long> findProjectIdsByToolId(@Param("toolId") Long toolId);
    
    @Query("SELECT a FROM Association a WHERE a.toolId IN :toolIds AND a.status = 'ACTIVE'")
    List<Association> findActiveByToolIds(@Param("toolIds") List<Long> toolIds);
    
    long countByStatus(String status);
    
    long countByProjectId(Long projectId);
    
    long countByInheritorId(Long inheritorId);
}