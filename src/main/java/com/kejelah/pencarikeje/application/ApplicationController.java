package com.kejelah.pencarikeje.application;

import com.kejelah.pencarikeje.application.dto.ApplicationDetailResponse;
import com.kejelah.pencarikeje.application.dto.ApplicationListItemResponse;
import com.kejelah.pencarikeje.application.dto.ApplicationRequest;
import com.kejelah.pencarikeje.application.dto.PageResponse;
import com.kejelah.pencarikeje.security.AuthenticatedUser;
import com.kejelah.pencarikeje.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "Applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @Operation(summary = "Create an application and its first APPLIED event")
    public ResponseEntity<ApplicationDetailResponse> create(@CurrentUser AuthenticatedUser user,
                                                            @Valid @RequestBody ApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.create(user.id(), request));
    }

    @GetMapping
    @Operation(summary = "List with search, status filter, sort and pagination")
    public PageResponse<ApplicationListItemResponse> list(
            @CurrentUser AuthenticatedUser user,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String statusCode,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String direction,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return applicationService.list(user.id(), q, statusCode, sort, direction, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Application detail including the full progress timeline")
    public ApplicationDetailResponse detail(@CurrentUser AuthenticatedUser user, @PathVariable Long id) {
        return applicationService.detail(user.id(), id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an application (current status is derived, not editable)")
    public ApplicationDetailResponse update(@CurrentUser AuthenticatedUser user,
                                            @PathVariable Long id,
                                            @Valid @RequestBody ApplicationRequest request) {
        return applicationService.update(user.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hard delete, cascading progress events and the stored resume")
    public ResponseEntity<Void> delete(@CurrentUser AuthenticatedUser user, @PathVariable Long id) {
        applicationService.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }
}
