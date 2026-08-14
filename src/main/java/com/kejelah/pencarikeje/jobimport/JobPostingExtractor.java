package com.kejelah.pencarikeje.jobimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * Pulls company/role/description out of a fetched page's {@code schema.org
 * JobPosting} JSON-LD block — the structured-data markup most job boards embed
 * for Google Jobs SEO. Purely parsing, no network access, so this one
 * implementation covers LinkedIn, JobStreet, and Workday (and anything else
 * following the same convention) instead of three site-specific scrapers.
 *
 * <p>Best-effort by design: if no JobPosting block is found, {@link #extract}
 * returns all-null fields rather than throwing. The page may genuinely not
 * have one, or may be serving a stripped-down response to a non-browser
 * request in the first place.
 */
@Component
public class JobPostingExtractor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Extracted extract(String html) {
        Document document = Jsoup.parse(html);
        for (Element script : document.select("script[type=application/ld+json]")) {
            JsonNode jobPosting = findJobPosting(parseQuietly(script.data()));
            if (jobPosting != null) {
                return toExtracted(jobPosting);
            }
        }
        return new Extracted(null, null, null);
    }

    private JsonNode parseQuietly(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            return null;
        }
    }

    /** JSON-LD shows up as a bare object, an array of objects, or an object with an "@graph" array. */
    private JsonNode findJobPosting(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isArray()) {
            for (JsonNode candidate : node) {
                JsonNode found = findJobPosting(candidate);
                if (found != null) {
                    return found;
                }
            }
            return null;
        }
        if (isJobPosting(node)) {
            return node;
        }
        if (node.has("@graph")) {
            return findJobPosting(node.get("@graph"));
        }
        return null;
    }

    private boolean isJobPosting(JsonNode node) {
        JsonNode type = node.get("@type");
        if (type == null) {
            return false;
        }
        if (type.isArray()) {
            for (JsonNode entry : type) {
                if ("JobPosting".equalsIgnoreCase(entry.asText())) {
                    return true;
                }
            }
            return false;
        }
        return "JobPosting".equalsIgnoreCase(type.asText());
    }

    private Extracted toExtracted(JsonNode jobPosting) {
        String title = textOrNull(jobPosting.get("title"));
        String company = organizationName(jobPosting.get("hiringOrganization"));
        String description = htmlToPlainText(textOrNull(jobPosting.get("description")));
        return new Extracted(company, title, description);
    }

    private String organizationName(JsonNode hiringOrganization) {
        if (hiringOrganization == null || hiringOrganization.isNull()) {
            return null;
        }
        if (hiringOrganization.isTextual()) {
            return textOrNull(hiringOrganization);
        }
        return textOrNull(hiringOrganization.get("name"));
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        String text = node.asText().trim();
        return text.isBlank() ? null : text;
    }

    /**
     * JobPosting {@code description} is typically HTML-flavoured rich text, and
     * this ends up in a plain textarea — block-level tags become newlines here
     * instead of being stripped invisibly, which would run every paragraph and
     * list item together into one unreadable line.
     */
    private String htmlToPlainText(String html) {
        if (html == null) {
            return null;
        }
        Document fragment = Jsoup.parseBodyFragment(html);
        fragment.select("br").before("\\n");
        fragment.select("p, div, li, tr").before("\\n");
        String withMarkers = fragment.body().wholeText();
        String text = withMarkers.replace("\\n", "\n")
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
        return text.isEmpty() ? null : text;
    }

    public record Extracted(String companyName, String roleName, String jobDescription) {
    }
}
