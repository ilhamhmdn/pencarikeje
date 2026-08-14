package com.kejelah.pencarikeje.jobimport;

import com.kejelah.pencarikeje.jobimport.dto.JobImportRequest;
import com.kejelah.pencarikeje.jobimport.dto.JobImportResponse;
import com.kejelah.pencarikeje.security.AuthenticatedUser;
import com.kejelah.pencarikeje.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Best-effort job posting import (MVP.md IMP-01..03). Nothing here touches a
 * specific application, so {@code user} exists only to keep this endpoint
 * behind auth like every other one — {@code SecurityConfig}'s
 * {@code anyRequest().authenticated()} already covers it, no explicit rule needed.
 */
@RestController
@RequestMapping("/api/job-imports")
@Tag(name = "Job import")
public class JobImportController {

    private final JobImportService jobImportService;

    public JobImportController(JobImportService jobImportService) {
        this.jobImportService = jobImportService;
    }

    @PostMapping
    @Operation(summary = "Fetch a job posting URL and extract company/role/description, best-effort")
    public JobImportResponse importFrom(@CurrentUser AuthenticatedUser user, @Valid @RequestBody JobImportRequest request) {
        return jobImportService.importFrom(request.url());
    }
}
