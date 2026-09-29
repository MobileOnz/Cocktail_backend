package com.application.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SensitiveLogMaskerTest {

    @Test
    void masksPersonalInformationWithoutRemovingOperationalContext() {
        assertThat(SensitiveLogMasker.maskIdentifier("google-123456789")).isEqualTo("go***89");
        assertThat(SensitiveLogMasker.maskMemberId(12345L)).isEqualTo("***45");
        assertThat(SensitiveLogMasker.maskName("홍길동")).isEqualTo("홍*동");
        assertThat(SensitiveLogMasker.maskName("소원")).isEqualTo("소*");
        assertThat(SensitiveLogMasker.maskEmail("tester@example.com"))
                .isEqualTo("t***@example.com");
        assertThat(SensitiveLogMasker.maskPhone("010-1234-5678"))
                .isEqualTo("010-****-5678");
    }

    @Test
    void handlesMissingAndShortValuesSafely() {
        assertThat(SensitiveLogMasker.maskIdentifier(null)).isEqualTo("-");
        assertThat(SensitiveLogMasker.maskIdentifier("abc")).isEqualTo("***");
        assertThat(SensitiveLogMasker.maskName("김")).isEqualTo("*");
        assertThat(SensitiveLogMasker.maskEmail("invalid")).isEqualTo("in***id");
        assertThat(SensitiveLogMasker.maskPhone("1234")).isEqualTo("****");
    }
}
