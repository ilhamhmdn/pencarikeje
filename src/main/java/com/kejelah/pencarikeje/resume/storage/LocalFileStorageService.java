package com.kejelah.pencarikeje.resume.storage;

import com.kejelah.pencarikeje.common.ApiException;
import com.kejelah.pencarikeje.common.ErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Filesystem implementation, laid out as {@code uploads/{userId}/{applicationId}/{uuid}.pdf}
 * (RES-02).
 *
 * <p>The stored name is always a generated UUID. The user-facing name lives in
 * {@code applications.resume_filename} instead, which removes filename collisions
 * and filename-based traversal risk entirely.
 */
@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final Path root;

    public LocalFileStorageService(StorageProperties properties) {
        this.root = Paths.get(properties.dir()).toAbsolutePath().normalize();
    }

    @Override
    public StoredFile store(Long userId, Long applicationId, MultipartFile file) {
        Path directory = root.resolve(Paths.get(String.valueOf(userId), String.valueOf(applicationId)));
        Path target = directory.resolve(UUID.randomUUID() + ".pdf");

        verifyWithinRoot(target);

        try {
            Files.createDirectories(directory);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            log.error("Failed to store resume for user {} application {}", userId, applicationId, ex);
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCodes.STORAGE_FAILURE, "Could not store the uploaded file.");
        }

        return new StoredFile(root.relativize(target).toString().replace('\\', '/'),
                sanitiseDisplayName(file.getOriginalFilename()));
    }

    @Override
    public Resource load(String storagePath) {
        Path target = root.resolve(storagePath).normalize();
        verifyWithinRoot(target);

        if (!Files.isReadable(target)) {
            throw ApiException.notFound(ErrorCodes.RESUME_NOT_FOUND, "The stored resume file is missing.");
        }
        return new PathResource(target);
    }

    @Override
    public void delete(String storagePath) {
        try {
            Path target = root.resolve(storagePath).normalize();
            verifyWithinRoot(target);
            Files.deleteIfExists(target);
        } catch (IOException | ApiException ex) {
            // An orphaned file is acceptable; an orphaned row is not (APP-08).
            log.warn("Could not delete stored file {}", storagePath, ex);
        }
    }

    /**
     * Every resolved path is checked against the uploads root before any I/O
     * (7.2). Storage keys come from our own database, but a corrupted or
     * hand-edited row must not be able to reach outside the root.
     */
    private void verifyWithinRoot(Path candidate) {
        if (!candidate.normalize().startsWith(root)) {
            throw ApiException.badRequest(ErrorCodes.STORAGE_FAILURE, "Resolved path escapes the storage root.");
        }
    }

    /**
     * Strips path separators, parent references and control characters from the
     * name we show back to the user (RES-01). This name is never used to build a
     * filesystem path.
     */
    static String sanitiseDisplayName(String original) {
        if (original == null || original.isBlank()) {
            return "resume.pdf";
        }
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replace("..", "").replaceAll("[\\p{Cntrl}]", "").trim();

        if (name.isBlank()) {
            return "resume.pdf";
        }
        return name.length() > 255 ? name.substring(0, 255) : name;
    }
}
