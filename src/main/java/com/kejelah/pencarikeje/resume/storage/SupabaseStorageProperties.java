package com.kejelah.pencarikeje.resume.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param url        Supabase project URL, e.g. {@code https://xyzcompany.supabase.co}
 * @param serviceKey Supabase service-role key (bypasses row-level security; server-side only)
 * @param bucket     name of the pre-created Storage bucket resumes are written to
 */
@ConfigurationProperties(prefix = "app.storage.supabase")
public record SupabaseStorageProperties(String url, String serviceKey, String bucket) {
}
