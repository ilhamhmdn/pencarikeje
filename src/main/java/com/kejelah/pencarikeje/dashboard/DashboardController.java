package com.kejelah.pencarikeje.dashboard;

import com.kejelah.pencarikeje.dashboard.dto.DashboardResponse;
import com.kejelah.pencarikeje.security.AuthenticatedUser;
import com.kejelah.pencarikeje.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    @Operation(summary = "Summary statistics for the authenticated user")
    public DashboardResponse dashboard(@CurrentUser AuthenticatedUser user) {
        return dashboardService.forUser(user.id());
    }
}
