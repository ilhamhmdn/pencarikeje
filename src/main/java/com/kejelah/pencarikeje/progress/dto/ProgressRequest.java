package com.kejelah.pencarikeje.progress.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * PRG-02 / PRG-03 payload.
 *
 * <p>There is no transition validation: any status may follow any status,
 * including ones that look terminal. Real recruitment processes are not state
 * machines (MVP.md 1.4).
 */
public record ProgressRequest(
        @NotNull(message = "statusId is required") Long statusId,
        @NotNull(message = "eventDate is required") LocalDate eventDate,
        String notes) {
}
