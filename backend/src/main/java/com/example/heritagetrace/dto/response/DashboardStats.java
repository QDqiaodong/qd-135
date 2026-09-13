package com.example.heritagetrace.dto.response;

import lombok.Data;

@Data
public class DashboardStats {
    private long toolCount;
    private long inheritorCount;
    private long projectCount;
    private long associationCount;
}