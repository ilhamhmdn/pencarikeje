package com.kejelah.pencarikeje.progress.dto;

import com.kejelah.pencarikeje.progress.ApplicationProgress;
import com.kejelah.pencarikeje.status.dto.StatusSummary;

import java.time.LocalDate;

/**
 * A timeline node (PRG-01, PRG-05).
 *
 * <p>{@code statusId} is included alongside the summary so the edit modal can
 * preselect the dropdown without a lookup by code.
 */
public record ProgressResponse(
        Long id,
        Long statusId,
        StatusSummary status,
        LocalDate eventDate,
        String notes) {

    public static ProgressResponse from(ApplicationProgress progress) {
        return new ProgressResponse(
                progress.getId(),
                progress.getStatus().getId(),
                StatusSummary.from(progress.getStatus()),
                progress.getEventDate(),
                progress.getNotes());
    }
}
