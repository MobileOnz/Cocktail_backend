package com.application.common.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Access Token 401 거부 사유 판별")
class JWTUtilFailureReasonTest {

    // 테스트 전용 더미 시크릿이다. 실제 운영 시크릿과 무관하다.
    private static final String SECRET = "test-only-jwt-secret-for-failure-reason-0123456789";
    private static final String OTHER_SECRET = "another-test-only-secret-for-invalid-token-012345";

    private final JWTUtil jwtUtil = new JWTUtil(SECRET);

    @Test
    @DisplayName("정상 토큰은 VALID, 만료 토큰은 EXPIRED로 구분한다")
    void distinguishesValidAndExpiredTokens() {
        // given
        String validToken = jwtUtil.createAccessJwt("uuid", "kakao_1", "ROLE_USER");
        String expiredToken = signedToken(SECRET, new Date(System.currentTimeMillis() - 60_000));

        // when & then
        assertThat(jwtUtil.getAccessTokenFailureReason(validToken)).isEqualTo("VALID");
        assertThat(jwtUtil.getAccessTokenFailureReason(expiredToken)).isEqualTo("EXPIRED");
    }

    @Test
    @DisplayName("다른 시크릿으로 서명됐거나 형식이 깨진 토큰은 INVALID로 구분한다")
    void classifiesForeignSignatureAndMalformedTokensAsInvalid() {
        // given: 시크릿 교체나 위조 상황을 가짜 토큰으로 재현한다.
        String foreignToken = signedToken(OTHER_SECRET, new Date(System.currentTimeMillis() + 60_000));

        // when & then
        assertThat(jwtUtil.getAccessTokenFailureReason(foreignToken)).isEqualTo("INVALID");
        assertThat(jwtUtil.getAccessTokenFailureReason("abc.def.ghi")).isEqualTo("INVALID");
    }

    private static String signedToken(String secret, Date expiration) {
        return Jwts.builder()
                .claim("uuid", "uuid")
                .claim("credentialId", "kakao_1")
                .claim("role", "ROLE_USER")
                .issuedAt(new Date(expiration.getTime() - 120_000))
                .expiration(expiration)
                .signWith(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .compact();
    }
}
