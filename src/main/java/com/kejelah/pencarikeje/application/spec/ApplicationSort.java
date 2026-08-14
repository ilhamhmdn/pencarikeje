package com.kejelah.pencarikeje.application.spec;

import com.kejelah.pencarikeje.common.ApiException;
import com.kejelah.pencarikeje.common.ErrorCodes;
import org.springframework.data.domain.Sort;

import java.util.Map;

/**
 * APP-05 sort whitelist.
 *
 * <p>An unrecognised key is a 400, never a silent fallback — silent fallbacks
 * hide frontend bugs.
 */
public final class ApplicationSort {

    public static final String DEFAULT_KEY = "dateApplied";

    /**
     * Maps the public sort key to an entity property path. {@code status} sorts by
     * the catalogue's presentation order rather than alphabetically, so the list
     * groups the way the dropdown reads.
     */
    private static final Map<String, String> SORTABLE = Map.of(
            "dateApplied", "dateApplied",
            "companyName", "companyName",
            "roleName", "roleName",
            "status", "status.displayOrder",
            "updatedAt", "updatedAt");

    private ApplicationSort() {
    }

    public static Sort resolve(String sortKey, String direction) {
        String key = (sortKey == null || sortKey.isBlank()) ? DEFAULT_KEY : sortKey.trim();

        String property = SORTABLE.get(key);
        if (property == null) {
            throw ApiException.badRequest(ErrorCodes.INVALID_SORT_KEY,
                    "Unsupported sort key '" + key + "'. Supported keys: " + SORTABLE.keySet().stream().sorted().toList());
        }

        Sort.Direction dir = resolveDirection(direction);

        // id is a deterministic tie-breaker: without it, equal sort values can
        // reorder between pages and silently drop or duplicate rows.
        return Sort.by(dir, property).and(Sort.by(Sort.Direction.DESC, "id"));
    }

    private static Sort.Direction resolveDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return Sort.Direction.DESC;
        }
        return switch (direction.trim().toLowerCase()) {
            case "asc" -> Sort.Direction.ASC;
            case "desc" -> Sort.Direction.DESC;
            default -> throw ApiException.badRequest(ErrorCodes.INVALID_SORT_KEY,
                    "Unsupported direction '" + direction + "'. Use 'asc' or 'desc'.");
        };
    }
}
