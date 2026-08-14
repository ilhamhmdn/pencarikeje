package com.kejelah.pencarikeje.application.spec;

import com.kejelah.pencarikeje.application.Application;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composable predicates for the applications list (MVP.md 8.1).
 *
 * <p>Only parameters that are actually present contribute a predicate, which
 * avoids a combinatorial explosion of repository methods.
 */
public final class ApplicationSpecifications {

    private ApplicationSpecifications() {
    }

    /**
     * SEC-01. Always the first predicate in the chain — scoping lives in the
     * query, never in a post-fetch check.
     */
    public static Specification<Application> ownedBy(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    /** APP-03: case-insensitive partial match across company and role. */
    public static Specification<Application> matches(String q) {
        if (q == null || q.isBlank()) {
            return null;
        }
        String pattern = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("companyName")), pattern),
                cb.like(cb.lower(root.get("roleName")), pattern));
    }

    /** APP-04: filter on the cached current status. */
    public static Specification<Application> hasStatusCode(String statusCode) {
        if (statusCode == null || statusCode.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status").get("code"), statusCode);
    }

    /**
     * Joins the status eagerly for content queries so rendering a page of rows
     * does not fire one status select per row (NFR-02).
     *
     * <p>The fetch is skipped for the count query, where a fetch join is invalid.
     */
    public static Specification<Application> withStatusFetched() {
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                root.fetch("status", JoinType.INNER);
            }
            return null;
        };
    }
}
