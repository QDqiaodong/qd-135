package com.example.heritagetrace.service;

import com.example.heritagetrace.dto.response.DashboardStats;
import com.example.heritagetrace.repository.AssociationRepository;
import com.example.heritagetrace.repository.InheritorRepository;
import com.example.heritagetrace.repository.ProjectRepository;
import com.example.heritagetrace.repository.ToolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    @Autowired
    private ToolRepository toolRepository;
    
    @Autowired
    private InheritorRepository inheritorRepository;
    
    @Autowired
    private ProjectRepository projectRepository;
    
    @Autowired
    private AssociationRepository associationRepository;
    
    public DashboardStats getStats() {
        DashboardStats stats = new DashboardStats();
        stats.setToolCount(toolRepository.count());
        stats.setInheritorCount(inheritorRepository.count());
        stats.setProjectCount(projectRepository.count());
        stats.setAssociationCount(associationRepository.countByStatus(com.example.heritagetrace.entity.Association.STATUS_ACTIVE));
        return stats;
    }
}