package com.kejelah.pencarikeje.resume;

import com.kejelah.pencarikeje.application.Application;
import com.kejelah.pencarikeje.application.ApplicationService;
import com.kejelah.pencarikeje.common.ApiException;
import com.kejelah.pencarikeje.common.ErrorCodes;
import com.kejelah.pencarikeje.resume.storage.FileStorageService;
import com.kejelah.pencarikeje.resume.storage.StorageProperties;
import com.kejelah.pencarikeje.resume.storage.StoredFile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
public class ResumeService {

    /** Leading bytes of every PDF. */
    private static final byte[] PDF_MAGIC = {'%', 'P', 'D', 'F'};

    private final ApplicationService applicationService;
    private final FileStorageService fileStorageService;
    private final long maxBytes;

    public ResumeService(ApplicationService applicationService,
                         FileStorageService fileStorageService,
                         StorageProperties storageProperties) {
        this.applicationService = applicationService;
        this.fileStorageService = fileStorageService;
        this.maxBytes = storageProperties.maxBytes();
    }

    /**
     * RES-01 and RES-03. Validation happens before anything is written, so a
     * rejected upload leaves no file behind.
     *
     * <p>Replacement order matters: the new file is written first, the row is
     * updated, and only then is the old file removed. A crash mid-operation can
     * strand a file, but must never leave the database pointing at a missing one.
     */
    @Transactional
    public ResumeMetadata upload(Long userId, Long applicationId, MultipartFile file) {
        Application application = applicationService.requireOwned(userId, applicationId);
        validate(file);

        String previousPath = application.getResumePath();

        StoredFile stored = fileStorageService.store(userId, applicationId, file);
        application.setResumePath(stored.storagePath());
        application.setResumeFilename(stored.displayName());

        if (previousPath != null && !previousPath.equals(stored.storagePath())) {
            fileStorageService.delete(previousPath);
        }

        return new ResumeMetadata(stored.displayName());
    }

    /**
     * RES-04. Ownership is verified before the file is opened — a non-owner
     * triggers no file I/O at all.
     */
    @Transactional(readOnly = true)
    public ResumeDownload download(Long userId, Long applicationId) {
        Application application = applicationService.requireOwned(userId, applicationId);

        if (application.getResumePath() == null) {
            throw ApiException.notFound(ErrorCodes.RESUME_NOT_FOUND, "This application has no resume.");
        }

        Resource resource = fileStorageService.load(application.getResumePath());
        return new ResumeDownload(resource, application.getResumeFilename());
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest(ErrorCodes.INVALID_FILE_TYPE, "No file was supplied.");
        }
        if (file.getSize() > maxBytes) {
            throw ApiException.badRequest(ErrorCodes.FILE_TOO_LARGE, "Resume must be 5 MB or smaller.");
        }
        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw ApiException.badRequest(ErrorCodes.INVALID_FILE_TYPE, "Resume must be a PDF.");
        }
        if (!hasPdfMagicBytes(file)) {
            // The declared MIME type is caller-controlled, so the actual bytes
            // decide (RES-01).
            throw ApiException.badRequest(ErrorCodes.INVALID_FILE_TYPE,
                    "File content is not a valid PDF.");
        }
    }

    private boolean hasPdfMagicBytes(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] header = in.readNBytes(PDF_MAGIC.length);
            return java.util.Arrays.equals(header, PDF_MAGIC);
        } catch (IOException ex) {
            return false;
        }
    }

    public record ResumeMetadata(String resumeFilename) {
    }

    public record ResumeDownload(Resource resource, String filename) {
    }
}
