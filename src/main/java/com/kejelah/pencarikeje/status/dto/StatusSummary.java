package com.kejelah.pencarikeje.status.dto;

import com.kejelah.pencarikeje.status.Status;

/** The {@code {code, name}} pair embedded wherever a current status is shown. */
public record StatusSummary(String code, String name) {

    public static StatusSummary from(Status status) {
        return new StatusSummary(status.getCode(), status.getName());
    }
}
