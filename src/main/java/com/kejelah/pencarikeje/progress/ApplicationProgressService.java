package com.kejelah.pencarikeje.progress;

import com.kejelah.pencarikeje.application.Application;
import com.kejelah.pencarikeje.application.ApplicationService;
import com.kejelah.pencarikeje.common.ApiException;
import com.kejelah.pencarikeje.common.ErrorCodes;
import com.kejelah.pencarikeje.progress.dto.ProgressRequest;
import com.kejelah.pencarikeje.progress.dto.ProgressResponse;
import com.kejelah.pencarikeje.status.Status;
import com.kejelah.pencarikeje.status.StatusService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Owns the progress timeline and, with it, the current-status cache.
 *
 * <p>This is the only class permitted to write {@code applications.status_id}
 * (MVP.md 3.3). Every mutation recomputes the cache inside the same transaction,
 * so the list view can never disagree with the timeline.
 */
@Service
public class ApplicationProgressService {

    private final ApplicationProgressRepository progressRepository;
    private final ApplicationService applicationService;
    private final StatusService statusService;

    public ApplicationProgressService(ApplicationProgressRepository progressRepository,
                                      ApplicationService applicationService,
                                      StatusService statusService) {
        this.progressRepository = progressRepository;
        this.applicationService = applicationService;
        this.statusService = statusService;
    }

    @Transactional(readOnly = true)
    public List<ProgressResponse> list(Long userId, Long applicationId) {
        applicationService.requireOwned(userId, applicationId);
        return progressRepository.findTimeline(applicationId).stream()
                .map(ProgressResponse::from)
                .toList();
    }

    /**
     * PRG-02. No transition validation: {@code REJECTED → RECONSIDERED} and
     * {@code REJECTED → INTERVIEW} are both accepted.
     */
    @Transactional
    public ProgressResponse add(Long userId, Long applicationId, ProgressRequest request) {
        Application application = applicationService.requireOwned(userId, applicationId);
        Status status = statusService.requireActiveById(request.statusId());

        ApplicationProgress event = progressRepository.save(new ApplicationProgress(
                application, status, request.eventDate(), blankToNull(request.notes())));

        recomputeCurrentStatus(application);
        return ProgressResponse.from(event);
    }

    /** PRG-03. Status, date and notes are all editable. */
    @Transactional
    public ProgressResponse update(Long userId, Long applicationId, Long progressId, ProgressRequest request) {
        Application application = applicationService.requireOwned(userId, applicationId);
        ApplicationProgress event = requireEvent(userId, applicationId, progressId);

        event.setStatus(statusService.requireActiveById(request.statusId()));
        event.setEventDate(request.eventDate());
        event.setNotes(blankToNull(request.notes()));

        recomputeCurrentStatus(application);
        return ProgressResponse.from(event);
    }

    /**
     * PRG-04. An application must always retain at least one progress event —
     * without one, the current-status cache would have no source of truth.
     */
    @Transactional
    public void delete(Long userId, Long applicationId, Long progressId) {
        Application application = applicationService.requireOwned(userId, applicationId);
        ApplicationProgress event = requireEvent(userId, applicationId, progressId);

        if (progressRepository.countByApplicationId(applicationId) <= 1) {
            throw ApiException.conflict(ErrorCodes.LAST_PROGRESS_EVENT,
                    "An application must keep at least one progress event.");
        }

        progressRepository.delete(event);
        progressRepository.flush();

        recomputeCurrentStatus(application);
    }

    /**
     * Rewrites the denormalised cache from the latest event, ordered by
     * {@code event_date DESC, id DESC} (MVP.md 3.3).
     *
     * <p>Runs inside the caller's transaction. The flush before reading matters:
     * a pending insert or update must be visible to the ordering query, or the
     * cache would be recomputed from stale rows.
     */
    private void recomputeCurrentStatus(Application application) {
        progressRepository.flush();

        List<ApplicationProgress> latestFirst = progressRepository.findLatestFirst(application.getId());
        if (latestFirst.isEmpty()) {
            // Unreachable while PRG-04 holds; failing loudly beats silently
            // leaving a stale status behind.
            throw new IllegalStateException(
                    "Application " + application.getId() + " has no progress events to derive a status from.");
        }

        application.applyRecomputedStatus(latestFirst.get(0).getStatus());
        application.setUpdatedAt(Instant.now());
    }

    /**
     * SEC-03. Verifies the event belongs to the application <em>and</em> the
     * application belongs to the caller, in the query itself.
     */
    private ApplicationProgress requireEvent(Long userId, Long applicationId, Long progressId) {
        return progressRepository.findByIdAndApplicationIdAndUserId(progressId, applicationId, userId)
                .orElseThrow(() -> ApiException.notFound(
                        ErrorCodes.PROGRESS_NOT_FOUND, "Progress event not found for this application."));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
