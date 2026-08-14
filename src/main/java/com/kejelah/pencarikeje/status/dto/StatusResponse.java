package com.kejelah.pencarikeje.status.dto;

import com.kejelah.pencarikeje.status.Status;

public record StatusResponse(Long id, String code, String name, Integer displayOrder) {

    public static StatusResponse from(Status status) {
        return new StatusResponse(status.getId(), status.getCode(), status.getName(), status.getDisplayOrder());
    }
}
