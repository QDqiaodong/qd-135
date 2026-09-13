package com.example.heritagetrace.dto.request;

import lombok.Data;

@Data
public class InheritorUpdateRequest {
    private String name;
    
    private String title;
    
    private String specialty;
    
    private String contact;
}