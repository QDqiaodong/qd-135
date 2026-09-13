package com.example.heritagetrace.repository;

import com.example.heritagetrace.entity.Tool;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToolRepository extends JpaRepository<Tool, Long> {
    Optional<Tool> findByToolNumber(String toolNumber);
    
    boolean existsByToolNumber(String toolNumber);
    
    Page<Tool> findByToolNameContainingOrToolNumberContaining(String toolName, String toolNumber, Pageable pageable);
    
    Page<Tool> findByCraftType(String craftType, Pageable pageable);
    
    @Query("SELECT t FROM Tool t WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "t.toolName LIKE %:keyword% OR t.toolNumber LIKE %:keyword% OR " +
           "t.craftType LIKE %:keyword% OR t.material LIKE %:keyword%)")
    Page<Tool> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    List<Tool> findByIdIn(List<Long> ids);
}