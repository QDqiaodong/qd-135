package com.example.heritagetrace.repository;

import com.example.heritagetrace.entity.AssociationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssociationHistoryRepository extends JpaRepository<AssociationHistory, Long> {
    List<AssociationHistory> findByAssociationId(Long associationId);
    
    List<AssociationHistory> findByAssociationIdOrderByActionTimeDesc(Long associationId);
    
    List<AssociationHistory> findByActionType(String actionType);
    
    List<AssociationHistory> findByActionTypeOrderByActionTimeDesc(String actionType);
}