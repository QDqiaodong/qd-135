package com.example.heritagetrace.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TraceabilityResult {
    private MainEntity mainEntity;
    private List<ToolDTO> associatedTools = new ArrayList<>();
    private List<InheritorDTO> associatedInheritors = new ArrayList<>();
    private List<ProjectDTO> associatedProjects = new ArrayList<>();
    private List<AssociationDTO> associations = new ArrayList<>();
    
    @Data
    public static class MainEntity {
        private Long id;
        private String name;
        private String type;
        
        public MainEntity(Long id, String name, String type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }
    }
}