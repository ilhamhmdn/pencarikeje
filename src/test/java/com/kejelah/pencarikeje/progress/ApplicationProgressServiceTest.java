package com.kejelah.pencarikeje.progress;

import com.kejelah.pencarikeje.application.Application;
import com.kejelah.pencarikeje.application.ApplicationService;
import com.kejelah.pencarikeje.common.ApiException;
import com.kejelah.pencarikeje.common.ErrorCodes;
import com.kejelah.pencarikeje.progress.dto.ProgressRequest;
import com.kejelah.pencarikeje.status.Status;
import com.kejelah.pencarikeje.status.StatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * NFR-04. Current-status recomputation and the last-event rule are the two rules
 * that keep the denormalised cache honest, so both are covered directly.
 */
class ApplicationProgressServiceTest {

    private ApplicationProgressRepository progressRepository;
    private ApplicationService applicationService;
    private StatusService statusService;
    private ApplicationProgressService service;

    private Application application;

    @BeforeEach
    void setUp() {
        progressRepository = mock(ApplicationProgressRepository.class);
        applicationService = mock(ApplicationService.class);
        statusService = mock(StatusService.class);
        service = new ApplicationProgressService(progressRepository, applicationService, statusService);

        application = mock(Application.class);
        when(application.getId()).thenReturn(42L);
        when(applicationService.requireOwned(1L, 42L)).thenReturn(application);
    }

    @Test
    @DisplayName("adding an event rewrites the cached status from the latest event")
    void addRecomputesCachedStatus() {
        Status interview = statusNamed("INTERVIEW");
        when(statusService.requireActiveById(4L)).thenReturn(interview);

        ApplicationProgress saved = new ApplicationProgress(
                application, interview, LocalDate.of(2026, 8, 1), null);
        when(progressRepository.save(any())).thenReturn(saved);
        when(progressRepository.findLatestFirst(42L)).thenReturn(List.of(saved));

        service.add(1L, 42L, new ProgressRequest(4L, LocalDate.of(2026, 8, 1), null));

        verify(application).applyRecomputedStatus(interview);
    }

    /**
     * MVP.md 1.4 / PRG-02: the system deliberately does not enforce a workflow.
     * A status that looks terminal may be followed by anything.
     */
    @Test
    @DisplayName("REJECTED may be followed by RECONSIDERED")
    void allowsNonLinearTransitions() {
        Status reconsidered = statusNamed("RECONSIDERED");
        when(statusService.requireActiveById(10L)).thenReturn(reconsidered);

        ApplicationProgress rejected = new ApplicationProgress(
                application, statusNamed("REJECTED"), LocalDate.of(2026, 7, 1), null);
        ApplicationProgress saved = new ApplicationProgress(
                application, reconsidered, LocalDate.of(2026, 8, 1), null);

        when(progressRepository.save(any())).thenReturn(saved);
        when(progressRepository.findLatestFirst(42L)).thenReturn(List.of(saved, rejected));

        assertThatCode(() -> service.add(1L, 42L, new ProgressRequest(10L, LocalDate.of(2026, 8, 1), null)))
                .doesNotThrowAnyException();

        verify(application).applyRecomputedStatus(reconsidered);
    }

    @Test
    @DisplayName("the last remaining progress event cannot be deleted")
    void refusesToDeleteFinalEvent() {
        ApplicationProgress only = new ApplicationProgress(
                application, statusNamed("APPLIED"), LocalDate.of(2026, 8, 1), null);

        when(progressRepository.findByIdAndApplicationIdAndUserId(7L, 42L, 1L))
                .thenReturn(java.util.Optional.of(only));
        when(progressRepository.countByApplicationId(42L)).thenReturn(1L);

        assertThatThrownBy(() -> service.delete(1L, 42L, 7L))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus().value()).isEqualTo(409);
                    assertThat(api.getCode()).isEqualTo(ErrorCodes.LAST_PROGRESS_EVENT);
                });

        verify(progressRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleting a non-final event recomputes the cache from what remains")
    void deleteRecomputesFromRemainingEvents() {
        Status applied = statusNamed("APPLIED");
        ApplicationProgress first = new ApplicationProgress(application, applied, LocalDate.of(2026, 7, 1), null);
        ApplicationProgress second = new ApplicationProgress(
                application, statusNamed("INTERVIEW"), LocalDate.of(2026, 8, 1), null);

        when(progressRepository.findByIdAndApplicationIdAndUserId(8L, 42L, 1L))
                .thenReturn(java.util.Optional.of(second));
        when(progressRepository.countByApplicationId(42L)).thenReturn(2L);
        when(progressRepository.findLatestFirst(42L)).thenReturn(List.of(first));

        service.delete(1L, 42L, 8L);

        verify(progressRepository).delete(second);
        verify(application).applyRecomputedStatus(applied);
    }

    @Test
    @DisplayName("an event belonging to another application is not found")
    void refusesEventFromAnotherApplication() {
        when(progressRepository.findByIdAndApplicationIdAndUserId(99L, 42L, 1L))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.delete(1L, 42L, 99L))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> assertThat(((ApiException) ex).getCode()).isEqualTo(ErrorCodes.PROGRESS_NOT_FOUND));
    }

    private Status statusNamed(String code) {
        Status status = mock(Status.class);
        when(status.getCode()).thenReturn(code);
        return status;
    }
}
