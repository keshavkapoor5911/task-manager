package com.taskmanager.controller;

import com.taskmanager.dto.DashboardStatsResponse;
import com.taskmanager.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/my-tasks")
    public ResponseEntity<DashboardStatsResponse> getMyTasksStats(Authentication authentication) {
        return ResponseEntity.ok(dashboardService.getMyTasksStats(authentication.getName()));
    }

    @GetMapping("/projects/{projectId}")
    public ResponseEntity<DashboardStatsResponse> getProjectDashboardStats(@PathVariable Long projectId, Authentication authentication) {
        return ResponseEntity.ok(dashboardService.getProjectDashboardStats(projectId, authentication.getName()));
    }
}
