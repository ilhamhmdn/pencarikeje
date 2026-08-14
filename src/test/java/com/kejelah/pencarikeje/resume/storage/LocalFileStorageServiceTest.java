package com.kejelah.pencarikeje.resume.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** RES-01 filename sanitisation. */
class LocalFileStorageServiceTest {

    @Test
    void stripsDirectoryComponents() {
        assertThat(LocalFileStorageService.sanitiseDisplayName("C:\\Users\\am\\cv.pdf")).isEqualTo("cv.pdf");
        assertThat(LocalFileStorageService.sanitiseDisplayName("/etc/passwd")).isEqualTo("passwd");
    }

    @Test
    void stripsParentReferences() {
        assertThat(LocalFileStorageService.sanitiseDisplayName("../../secret.pdf")).isEqualTo("secret.pdf");
    }

    @Test
    void stripsControlCharacters() {
        assertThat(LocalFileStorageService.sanitiseDisplayName("cv\u0000\u0007.pdf")).isEqualTo("cv.pdf");
    }

    @Test
    void fallsBackWhenNameIsAbsentOrEmptied() {
        assertThat(LocalFileStorageService.sanitiseDisplayName(null)).isEqualTo("resume.pdf");
        assertThat(LocalFileStorageService.sanitiseDisplayName("   ")).isEqualTo("resume.pdf");
        assertThat(LocalFileStorageService.sanitiseDisplayName("..")).isEqualTo("resume.pdf");
    }

    @Test
    void keepsOrdinaryNamesIntact() {
        assertThat(LocalFileStorageService.sanitiseDisplayName("Ilham CV 2026.pdf")).isEqualTo("Ilham CV 2026.pdf");
    }
}
