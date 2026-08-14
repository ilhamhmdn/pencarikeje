package com.kejelah.pencarikeje.resume.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param dir      storage root; relative paths resolve against the working directory
 * @param maxBytes hard cap enforced in code as well as by the multipart limits
 */
@ConfigurationProperties(prefix = "app.upload")
public record StorageProperties(String dir, long maxBytes) {
}
