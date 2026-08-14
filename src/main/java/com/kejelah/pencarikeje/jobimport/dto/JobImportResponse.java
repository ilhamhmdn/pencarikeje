package com.kejelah.pencarikeje.jobimport.dto;

/**
 * Best-effort extraction result (MVP.md IMP-03): any or all fields may be
 * {@code null} when the page had no parseable JobPosting data. The frontend
 * prefills whatever came back and leaves the rest for manual entry — nothing
 * about this response is ever saved automatically.
 */
public record JobImportResponse(String companyName, String roleName, String jobDescription) {
}
