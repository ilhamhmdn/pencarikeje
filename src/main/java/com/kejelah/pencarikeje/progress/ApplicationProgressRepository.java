package com.kejelah.pencarikeje.progress;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Progress lookups are scoped by application <em>and</em> by owner.
 *
 * <p>SEC-03: verifying only the parent application is insufficient — the event
 * must belong to the application and the application must belong to the caller.
 */
public interface ApplicationProgressRepository extends JpaRepository<ApplicationProgress, Long> {

    /** Timeline ordering: event_date ASC, id ASC (PRG-01). */
    @Query("""
            select p from ApplicationProgress p join fetch p.status
            where p.application.id = :applicationId
            order by p.eventDate asc, p.id asc
            """)
    List<ApplicationProgress> findTimeline(Long applicationId);

    @Query("""
            select p from ApplicationProgress p join fetch p.status
            where p.id = :id and p.application.id = :applicationId and p.application.user.id = :userId
            """)
    Optional<ApplicationProgress> findByIdAndApplicationIdAndUserId(Long id, Long applicationId, Long userId);

    /**
     * Current status source of truth: latest event by event_date DESC, id DESC
     * (MVP.md 3.3). Returns a list so the caller can take the first element
     * without an exception when the application has no events left.
     */
    @Query("""
            select p from ApplicationProgress p join fetch p.status
            where p.application.id = :applicationId
            order by p.eventDate desc, p.id desc
            """)
    List<ApplicationProgress> findLatestFirst(Long applicationId);

    long countByApplicationId(Long applicationId);

    /** Bulk timeline load for a page of applications, avoiding an N+1 (NFR-02). */
    @Query("""
            select p from ApplicationProgress p join fetch p.status
            where p.application.id in :applicationIds
            order by p.eventDate asc, p.id asc
            """)
    List<ApplicationProgress> findTimelinesFor(List<Long> applicationIds);
}
