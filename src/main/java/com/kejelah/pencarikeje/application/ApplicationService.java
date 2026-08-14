package com.kejelah.pencarikeje.application;

import com.kejelah.pencarikeje.application.dto.ApplicationDetailResponse;
import com.kejelah.pencarikeje.application.dto.ApplicationListItemResponse;
import com.kejelah.pencarikeje.application.dto.ApplicationRequest;
import com.kejelah.pencarikeje.application.dto.PageResponse;
import com.kejelah.pencarikeje.application.spec.ApplicationSort;
import com.kejelah.pencarikeje.application.spec.ApplicationSpecifications;
import com.kejelah.pencarikeje.auth.User;
import com.kejelah.pencarikeje.auth.UserRepository;
import com.kejelah.pencarikeje.common.ApiException;
import com.kejelah.pencarikeje.common.ErrorCodes;
import com.kejelah.pencarikeje.progress.ApplicationProgress;
import com.kejelah.pencarikeje.progress.ApplicationProgressRepository;
import com.kejelah.pencarikeje.progress.dto.ProgressResponse;
import com.kejelah.pencarikeje.resume.storage.FileStorageService;
import com.kejelah.pencarikeje.status.Status;
import com.kejelah.pencarikeje.status.StatusService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ApplicationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ApplicationRepository applicationRepository;
    private final ApplicationProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final StatusService statusService;
    private final FileStorageService fileStorageService;

    public ApplicationService(ApplicationRepository applicationRepository,
                              ApplicationProgressRepository progressRepository,
                              UserRepository userRepository,
                              StatusService statusService,
                              FileStorageService fileStorageService) {
        this.applicationRepository = applicationRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.statusService = statusService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * APP-01. Inserts the application and its first {@code APPLIED} progress event
     * atomically: if either fails, both roll back.
     *
     * <p>This is the one place outside {@code ApplicationProgressService} that sets
     * the cached status, and it does so by establishing the invariant rather than
     * recomputing it — the application's status and its only event are the same row
     * value by construction (MVP.md 3.3).
     */
    @Transactional
    public ApplicationDetailResponse create(Long userId, ApplicationRequest request) {
        User user = userRepository.getReferenceById(userId);
        Status applied = statusService.requireByCode(StatusService.APPLIED);

        Application application = new Application(
                user, request.companyName().trim(), request.roleName().trim(), request.dateApplied(), applied);
        applyEditableFields(application, request);

        application = applicationRepository.save(application);

        ApplicationProgress firstEvent = new ApplicationProgress(
                application, applied, request.dateApplied(), null);
        progressRepository.save(firstEvent);

        return ApplicationDetailResponse.of(application, List.of(ProgressResponse.from(firstEvent)));
    }

    /** APP-02 through APP-05. */
    @Transactional(readOnly = true)
    public PageResponse<ApplicationListItemResponse> list(Long userId,
                                                          String q,
                                                          String statusCode,
                                                          String sort,
                                                          String direction,
                                                          int page,
                                                          int size) {

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                ApplicationSort.resolve(sort, direction));

        Specification<Application> spec = ApplicationSpecifications.ownedBy(userId)
                .and(ApplicationSpecifications.matches(q))
                .and(ApplicationSpecifications.hasStatusCode(statusCode))
                .and(ApplicationSpecifications.withStatusFetched());

        Page<Application> result = applicationRepository.findAll(spec, pageable);
        return PageResponse.from(result, ApplicationListItemResponse::from);
    }

    /** APP-06. One response carrying the application and its full timeline. */
    @Transactional(readOnly = true)
    public ApplicationDetailResponse detail(Long userId, Long applicationId) {
        Application application = requireOwned(userId, applicationId);
        List<ProgressResponse> timeline = progressRepository.findTimeline(applicationId).stream()
                .map(ProgressResponse::from)
                .toList();
        return ApplicationDetailResponse.of(application, timeline);
    }

    /**
     * APP-07. Editing {@code dateApplied} deliberately does not retroactively
     * alter the initial progress event's date; the two are independent once
     * created.
     */
    @Transactional
    public ApplicationDetailResponse update(Long userId, Long applicationId, ApplicationRequest request) {
        Application application = requireOwned(userId, applicationId);

        application.setCompanyName(request.companyName().trim());
        application.setRoleName(request.roleName().trim());
        application.setDateApplied(request.dateApplied());
        applyEditableFields(application, request);

        List<ProgressResponse> timeline = progressRepository.findTimeline(applicationId).stream()
                .map(ProgressResponse::from)
                .toList();
        return ApplicationDetailResponse.of(application, timeline);
    }

    /**
     * APP-08. Hard delete. The database cascades progress rows; the stored resume
     * is removed afterwards on a best-effort basis, because a failed file delete
     * must not roll back the transaction.
     */
    @Transactional
    public void delete(Long userId, Long applicationId) {
        Application application = requireOwned(userId, applicationId);
        String resumePath = application.getResumePath();

        applicationRepository.delete(application);

        if (resumePath != null) {
            fileStorageService.delete(resumePath);
        }
    }

    /**
     * The single owner-scoped lookup used by every mutating path here and by the
     * resume and progress services.
     *
     * <p>A row belonging to somebody else is a 403, not a 404: the caller is
     * authenticated and the resource does exist (MVP.md 6.3, SEC-02).
     */
    @Transactional(readOnly = true)
    public Application requireOwned(Long userId, Long applicationId) {
        return applicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> applicationRepository.existsById(applicationId)
                        ? ApiException.forbidden(ErrorCodes.APPLICATION_ACCESS_DENIED,
                        "You do not have access to this application.")
                        : ApiException.notFound(ErrorCodes.APPLICATION_NOT_FOUND,
                        "Application not found."));
    }

    private void applyEditableFields(Application application, ApplicationRequest request) {
        application.setJobDescription(blankToNull(request.jobDescription()));
        application.setPortalUrl(blankToNull(request.portalUrl()));
        application.setNotes(blankToNull(request.notes()));
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
