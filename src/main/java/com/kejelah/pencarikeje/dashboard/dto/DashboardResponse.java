package com.kejelah.pencarikeje.dashboard.dto;

import com.kejelah.pencarikeje.application.dto.ApplicationListItemResponse;

import java.util.List;

/**
 * DASH-01. A summary, not a second applications table — deliberately without
 * pagination or filtering (DASH-02).
 */
public record DashboardResponse(
        long totalApplications,
        long interviewCount,
        long offerCount,
        List<StatusCount> statusBreakdown,
        List<ApplicationListItemResponse> recentApplications) {

    public record StatusCount(String statusCode, String statusName, long count) {
    }
}
