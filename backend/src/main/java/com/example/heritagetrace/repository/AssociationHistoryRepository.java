package com.example.heritagetrace.repository;

import com.example.heritagetrace.entity.AssociationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssociationHistoryRepository extends JpaRepository<AssociationHistory, Long> {
    List<AssociationHistory> findByAssociationId(Long associationId);

    List<AssociationHistory> findByAssociationIdOrderByActionTimeDesc(Long associationId);

    List<AssociationHistory> findByActionType(String actionType);

    List<AssociationHistory> findByActionTypeOrderByActionTimeDesc(String actionType);

    @Query("SELECT h FROM AssociationHistory h WHERE " +
            "(:toolId IS NULL OR h.toolId = :toolId) AND " +
            "(:inheritorId IS NULL OR h.oldInheritorId = :inheritorId OR h.newInheritorId = :inheritorId) AND " +
            "(:projectId IS NULL OR h.oldProjectId = :projectId OR h.newProjectId = :projectId) " +
            "ORDER BY h.actionTime ASC, h.id ASC")
    List<AssociationHistory> findLedger(@Param("toolId") Long toolId,
                                        @Param("inheritorId") Long inheritorId,
                                        @Param("projectId") Long projectId);
}
