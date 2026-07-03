package com.bestech.authentification_service.controller;

import com.bestech.authentification_service.dto.DashboardStatsDto;
import com.bestech.authentification_service.service.LoginEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/stats")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AdminStatsController {

    private final LoginEventService loginEventService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsDto> getDashboard() {
        return ResponseEntity.ok(loginEventService.getDashboardStats());
    }
}
