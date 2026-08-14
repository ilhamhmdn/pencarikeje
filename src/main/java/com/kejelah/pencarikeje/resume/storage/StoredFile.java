package com.kejelah.pencarikeje.resume.storage;

/**
 * @param storagePath  opaque storage key persisted in {@code applications.resume_path}
 * @param displayName  sanitised original filename, shown to the user
 */
public record StoredFile(String storagePath, String displayName) {
}
