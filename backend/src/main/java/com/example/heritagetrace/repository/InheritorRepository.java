package com.example.heritagetrace.repository;

import com.example.heritagetrace.entity.Inheritor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InheritorRepository extends JpaRepository<Inheritor, Long> {
    Page<Inheritor> findByNameContaining(String name, Pageable pageable);
    
    Page<Inheritor> findByTitle(String title, Pageable pageable);
    
    @Query("SELECT i FROM Inheritor i WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "i.name LIKE %:keyword% OR i.title LIKE %:keyword% OR " +
           "i.specialty LIKE %:keyword%)")
    Page<Inheritor> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    List<Inheritor> findByIdIn(List<Long> ids);
}