package com.kejelah.pencarikeje.status;

import com.kejelah.pencarikeje.common.ApiException;
import com.kejelah.pencarikeje.common.ErrorCodes;
import com.kejelah.pencarikeje.status.dto.StatusResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StatusService {

    /** Seeded code for the event created alongside a new application (APP-01). */
    public static final String APPLIED = "APPLIED";

    private final StatusRepository statusRepository;

    public StatusService(StatusRepository statusRepository) {
        this.statusRepository = statusRepository;
    }

    @Transactional(readOnly = true)
    public List<StatusResponse> listActive() {
        return statusRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(StatusResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Status requireActiveById(Long id) {
        return statusRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> ApiException.badRequest(
                        ErrorCodes.STATUS_NOT_FOUND, "Unknown or inactive status id: " + id));
    }

    @Transactional(readOnly = true)
    public Status requireByCode(String code) {
        return statusRepository.findByCode(code)
                .orElseThrow(() -> ApiException.badRequest(
                        ErrorCodes.STATUS_NOT_FOUND, "Unknown status code: " + code));
    }
}
