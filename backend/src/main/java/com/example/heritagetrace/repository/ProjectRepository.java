package com.example.heritagetrace.repository;

import com.example.heritagetrace.entity.Project;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    /**
     * 阶段推进专用：SELECT ... FOR UPDATE 锁住项目行。
     * 两人同时点结项时，后到的事务会阻塞到先到的提交，再读到最新阶段。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Project p WHERE p.id = :id")
    Optional<Project> findByIdForUpdate(@Param("id") Long id);
}