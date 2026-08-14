package com.kejelah.pencarikeje.application;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Every finder here is scoped by owner in the query itself.
 *
 * <p>MVP.md 7.1 forbids fetching by id and comparing the user in the service —
 * a post-fetch check is one forgotten {@code if} away from an IDOR. Do not add an
 * unscoped finder to this interface; {@code findById} is inherited but must not be
 * used for request-driven lookups.
 */
public interface ApplicationRepository extends JpaRepository<Application, Long>,
        JpaSpecificationExecutor<Application> {

    Optional<Application> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    long countByUserIdAndStatusCodeIn(Long userId, List<String> statusCodes);

    /**
     * Detail view: fetches the application with its status in one query. The
     * timeline is loaded separately by the progress repository so that neither
     * query multiplies rows.
     */
    @Query("select a from Application a join fetch a.status where a.id = :id and a.user.id = :userId")
    Optional<Application> findDetailByIdAndUserId(Long id, Long userId);

    /** Dashboard status breakdown, ordered by display_order (DASH-01). */
    @Query("""
            select s.code, s.name, count(a)
            from Application a join a.status s
            where a.user.id = :userId
            group by s.code, s.name, s.displayOrder
            order by s.displayOrder asc
            """)
    List<Object[]> countByStatusForUser(Long userId);

    /** Five most recent by date_applied DESC, id DESC (DASH-01). */
    @Query("select a from Application a join fetch a.status where a.user.id = :userId order by a.dateApplied desc, a.id desc")
    List<Application> findRecentForUser(Long userId, Pageable pageable);
}
