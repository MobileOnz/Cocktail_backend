package com.application.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("운영 로그 개인정보 마스킹")
class SensitiveLogMaskerTest {

    @Test
    @DisplayName("가짜 개인정보를 운영에 필요한 형태로 마스킹한다")
    void masksPersonalInformationWithoutRemovingOperationalContext() {
        // given: 실제 회원정보가 아닌 테스트 전용 가짜 데이터다.
        String credentialId = "google-123456789";
        String name = "홍길동";
        String shortName = "소원";
        String email = "tester@example.com";
        String phone = "010-1234-5678";

        // when
        String maskedCredentialId = SensitiveLogMasker.maskIdentifier(credentialId);
        String maskedName = SensitiveLogMasker.maskName(name);
        String maskedShortName = SensitiveLogMasker.maskName(shortName);
        String maskedEmail = SensitiveLogMasker.maskEmail(email);
        String maskedPhone = SensitiveLogMasker.maskPhone(phone);

        // then
        assertThat(maskedCredentialId).isEqualTo("go***89");
        assertThat(maskedName).isEqualTo("홍*동");
        assertThat(maskedShortName).isEqualTo("소*");
        assertThat(maskedEmail).isEqualTo("t***@example.com");
        assertThat(maskedPhone).isEqualTo("010-****-5678");
    }

    @Test
    @DisplayName("누락되거나 짧은 값도 원문을 노출하지 않고 안전하게 처리한다")
    void handlesMissingAndShortValuesSafely() {
        // given
        String missingIdentifier = null;

        // when
        String maskedMissingIdentifier = SensitiveLogMasker.maskIdentifier(missingIdentifier);
        String maskedShortIdentifier = SensitiveLogMasker.maskIdentifier("abc");
        String maskedShortName = SensitiveLogMasker.maskName("김");
        String maskedInvalidEmail = SensitiveLogMasker.maskEmail("invalid");
        String maskedShortPhone = SensitiveLogMasker.maskPhone("1234");

        // then
        assertThat(maskedMissingIdentifier).isEqualTo("-");
        assertThat(maskedShortIdentifier).isEqualTo("***");
        assertThat(maskedShortName).isEqualTo("*");
        assertThat(maskedInvalidEmail).isEqualTo("in***id");
        assertThat(maskedShortPhone).isEqualTo("****");
    }
}
