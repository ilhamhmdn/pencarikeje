package com.kejelah.pencarikeje.application;

import com.kejelah.pencarikeje.auth.UserRepository;
import com.kejelah.pencarikeje.common.ApiException;
import com.kejelah.pencarikeje.common.ErrorCodes;
import com.kejelah.pencarikeje.progress.ApplicationProgressRepository;
import com.kejelah.pencarikeje.resume.storage.FileStorageService;
import com.kejelah.pencarikeje.status.StatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * NFR-04. The ownership check is the highest-priority requirement in the spec
 * (7.1), so its two failure shapes are pinned here.
 */
class ApplicationServiceOwnershipTest {

    private ApplicationRepository applicationRepository;
    private ApplicationService service;

    @BeforeEach
    void setUp() {
        applicationRepository = mock(ApplicationRepository.class);
        service = new ApplicationService(
                applicationRepository,
                mock(ApplicationProgressRepository.class),
                mock(UserRepository.class),
                mock(StatusService.class),
                mock(FileStorageService.class));
    }

    @Test
    void returnsTheApplicationWhenTheCallerOwnsIt() {
        Application owned = mock(Application.class);
        when(applicationRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(owned));

        assertThat(service.requireOwned(1L, 5L)).isSameAs(owned);
    }

    /**
     * 6.3 distinguishes the two: 403 means authenticated but not the owner, 404
     * means the row does not exist for anyone.
     */
    @Test
    void anotherUsersApplicationIsForbiddenNotMissing() {
        when(applicationRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());
        when(applicationRepository.existsById(5L)).thenReturn(true);

        assertThatThrownBy(() -> service.requireOwned(1L, 5L))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus().value()).isEqualTo(403);
                    assertThat(api.getCode()).isEqualTo(ErrorCodes.APPLICATION_ACCESS_DENIED);
                });
    }

    @Test
    void unknownApplicationIsNotFound() {
        when(applicationRepository.findByIdAndUserId(404L, 1L)).thenReturn(Optional.empty());
        when(applicationRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> service.requireOwned(1L, 404L))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus().value()).isEqualTo(404);
                    assertThat(api.getCode()).isEqualTo(ErrorCodes.APPLICATION_NOT_FOUND);
                });
    }
}
