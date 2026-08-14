package com.kejelah.pencarikeje.application.spec;

import com.kejelah.pencarikeje.common.ApiException;
import com.kejelah.pencarikeje.common.ErrorCodes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** APP-05: the whitelist must reject unknown keys rather than fall back silently. */
class ApplicationSortTest {

    @Test
    void defaultsToDateAppliedDescending() {
        Sort sort = ApplicationSort.resolve(null, null);

        Sort.Order first = sort.iterator().next();
        assertThat(first.getProperty()).isEqualTo("dateApplied");
        assertThat(first.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @ParameterizedTest
    @ValueSource(strings = {"dateApplied", "companyName", "roleName", "status", "updatedAt"})
    void acceptsEveryWhitelistedKey(String key) {
        assertThat(ApplicationSort.resolve(key, "asc")).isNotNull();
    }

    @Test
    void sortsStatusByPresentationOrderRatherThanAlphabetically() {
        Sort sort = ApplicationSort.resolve("status", "asc");

        assertThat(sort.iterator().next().getProperty()).isEqualTo("status.displayOrder");
    }

    @Test
    void appendsIdAsTieBreakerSoPagesCannotReorder() {
        Sort sort = ApplicationSort.resolve("companyName", "asc");

        assertThat(sort).extracting(Sort.Order::getProperty).containsExactly("companyName", "id");
    }

    @Test
    void rejectsUnknownSortKeyWith400() {
        assertThatThrownBy(() -> ApplicationSort.resolve("passwordHash", "asc"))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException api = (ApiException) ex;
                    assertThat(api.getStatus().value()).isEqualTo(400);
                    assertThat(api.getCode()).isEqualTo(ErrorCodes.INVALID_SORT_KEY);
                });
    }

    @Test
    void rejectsUnknownDirection() {
        assertThatThrownBy(() -> ApplicationSort.resolve("companyName", "sideways"))
                .isInstanceOf(ApiException.class);
    }
}
