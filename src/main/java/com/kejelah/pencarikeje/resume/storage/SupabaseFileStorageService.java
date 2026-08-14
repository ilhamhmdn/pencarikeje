package com.kejelah.pencarikeje.resume.storage;

import com.kejelah.pencarikeje.common.ApiException;
import com.kejelah.pencarikeje.common.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Stores resumes in a Supabase Storage bucket over its REST API (MVP.md 8.3).
 *
 * <p>Chosen for deployed environments whose local filesystem is ephemeral
 * (wiped on every redeploy or restart) — the storage key layout mirrors
 * {@link LocalFileStorageService} so nothing outside this class needs to know
 * which implementation is active. Selected via {@code app.storage.provider=supabase};
 * {@link LocalFileStorageService} remains the default.
 */
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "supabase")
public class SupabaseFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseFileStorageService.class);
    private static final MediaType PDF = MediaType.parseMediaType("application/pdf");

    private final RestClient restClient;
    private final String bucket;

    public SupabaseFileStorageService(SupabaseStorageProperties properties) {
        this.bucket = properties.bucket();
        this.restClient = RestClient.builder()
                .baseUrl(properties.url() + "/storage/v1/object")
                .defaultHeader("Authorization", "Bearer " + properties.serviceKey())
                .build();
    }

    @Override
    public StoredFile store(Long userId, Long applicationId, MultipartFile file) {
        String key = userId + "/" + applicationId + "/" + UUID.randomUUID() + ".pdf";

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCodes.STORAGE_FAILURE, "Could not read the uploaded file.");
        }

        try {
            restClient.put()
                    .uri("/{bucket}/{key}", bucket, key)
                    .header("x-upsert", "true")
                    .contentType(PDF)
                    .body(bytes)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.error("Failed to store resume for user {} application {}", userId, applicationId, ex);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCodes.STORAGE_FAILURE, "Could not store the uploaded file.");
        }

        return new StoredFile(key, LocalFileStorageService.sanitiseDisplayName(file.getOriginalFilename()));
    }

    @Override
    public Resource load(String storagePath) {
        try {
            byte[] bytes = restClient.get()
                    .uri("/{bucket}/{key}", bucket, storagePath)
                    .retrieve()
                    .body(byte[].class);
            if (bytes == null) {
                throw ApiException.notFound(ErrorCodes.RESUME_NOT_FOUND, "The stored resume file is missing.");
            }
            return new ByteArrayResource(bytes);
        } catch (RestClientResponseException ex) {
            throw ApiException.notFound(ErrorCodes.RESUME_NOT_FOUND, "The stored resume file is missing.");
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            restClient.delete()
                    .uri("/{bucket}/{key}", bucket, storagePath)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            // An orphaned file is acceptable; an orphaned row is not (APP-08).
            log.warn("Could not delete stored file {}", storagePath, ex);
        }
    }
}
