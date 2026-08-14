package com.kejelah.pencarikeje.resume.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Storage abstraction for resumes (MVP.md 8.1, 8.3).
 *
 * <p>Swapping the local filesystem for object storage means adding one
 * implementation and changing nothing else. Deployed environments need this
 * because most free-tier hosts have ephemeral filesystems that would silently
 * discard uploads on redeploy.
 */
public interface FileStorageService {

    /** Writes the file and returns its storage key. Never overwrites in place. */
    StoredFile store(Long userId, Long applicationId, MultipartFile file);

    Resource load(String storagePath);

    /**
     * Best-effort removal. Implementations log and swallow failures: an orphaned
     * file is acceptable, an orphaned row is not (APP-08).
     */
    void delete(String storagePath);
}
