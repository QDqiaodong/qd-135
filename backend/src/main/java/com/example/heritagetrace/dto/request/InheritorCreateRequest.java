package com.example.heritagetrace.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InheritorCreateRequest {
    @NotBlank(message = "传承人姓名不能为空")
    private String name;
    
    private String title;
    
    private String specialty;
    
    private String contact;
}