package com.kejelah.pencarikeje.application.dto;

import com.kejelah.pencarikeje.application.Application;
import com.kejelah.pencarikeje.status.dto.StatusSummary;

import java.time.Instant;
import java.time.LocalDate;

/** APP-02 list row. */
public record ApplicationListItemResponse(
        Long id,
        String companyName,
        String roleName,
        StatusSummary currentStatus,
        String portalUrl,
        LocalDate dateApplied,
        String resumeFilename,
        Instant updatedAt) {

    public static ApplicationListItemResponse from(Application application) {
        return new ApplicationListItemResponse(
                application.getId(),
                application.getCompanyName(),
                application.getRoleName(),
                StatusSummary.from(application.getStatus()),
                application.getPortalUrl(),
                application.getDateApplied(),
                application.getResumeFilename(),
                application.getUpdatedAt());
    }
}
