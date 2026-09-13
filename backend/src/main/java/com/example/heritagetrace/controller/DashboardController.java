package com.example.heritagetrace.controller;

import com.example.heritagetrace.dto.response.ApiResponse;
import com.example.heritagetrace.dto.response.DashboardStats;
import com.example.heritagetrace.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;
    
    @GetMapping("/stats")
    public ApiResponse<DashboardStats> getStats() {
        DashboardStats stats = dashboardService.getStats();
        return ApiResponse.success(stats);
    }
}