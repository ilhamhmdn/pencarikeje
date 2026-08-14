package com.kejelah.pencarikeje.application.dto;

import com.kejelah.pencarikeje.application.Application;
import com.kejelah.pencarikeje.progress.dto.ProgressResponse;
import com.kejelah.pencarikeje.status.dto.StatusSummary;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * APP-06. Carries the full timeline so the detail page loads in one round trip.
 */
public record ApplicationDetailResponse(
        Long id,
        String companyName,
        String roleName,
        String jobDescription,
        String portalUrl,
        LocalDate dateApplied,
        StatusSummary currentStatus,
        String resumeFilename,
        String notes,
        Instant createdAt,
        Instant updatedAt,
        List<ProgressResponse> progress) {

    public static ApplicationDetailResponse of(Application application, List<ProgressResponse> progress) {
        return new ApplicationDetailResponse(
                application.getId(),
                application.getCompanyName(),
                application.getRoleName(),
                application.getJobDescription(),
                application.getPortalUrl(),
                application.getDateApplied(),
                StatusSummary.from(application.getStatus()),
                application.getResumeFilename(),
                application.getNotes(),
                application.getCreatedAt(),
                application.getUpdatedAt(),
                progress);
    }
}
