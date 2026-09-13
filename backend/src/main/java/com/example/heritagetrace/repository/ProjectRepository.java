package com.example.heritagetrace.repository;

import com.example.heritagetrace.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByParentId(Long parentId);
    
    List<Project> findByParentIdIsNull();
    
    List<Project> findByCategory(String category);
    
    @Query("SELECT p FROM Project p WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "p.name LIKE %:keyword% OR p.category LIKE %:keyword%)")
    List<Project> searchByKeyword(@Param("keyword") String keyword);
    
    List<Project> findByIdIn(List<Long> ids);
}