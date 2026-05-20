package com.application.web.services.bookmark;

import com.application.common.auth.jwt.JWTUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JWT 유틸리티 단위 테스트 (Mock 기반)
 * - Spring Context 로드 없이 JWT 생성/검증 로직 테스트
 */
@DisplayName("JWT 유틸리티 단위 테스트")
class JwtTest {

    private JWTUtil jwtUtil;
    private static final String TEST_SECRET = "test-secret-key-for-unit-testing-must-be-at-least-32-characters-long";

    @BeforeEach
    void setUp() {
        // 테스트용 secret으로 JWTUtil 직접 생성 (Spring Context 불필요)
        jwtUtil = new JWTUtil(TEST_SECRET);
    }

    @Test
    @DisplayName("Access Token 생성 시 유효한 토큰 반환")
    void createAccessJwt_ReturnsValidToken() {
        // given
        String uuid = UUID.randomUUID().toString();
        String credentialId = "test-user-id";
        String role = "USER";

        // when
        String token = jwtUtil.createAccessJwt(uuid, credentialId, role);

        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT 형식: header.payload.signature
    }

    @Test
    @DisplayName("생성된 토큰에서 credentialId 추출")
    void getCredentialId_FromToken_ReturnsCorrectValue() {
        // given
        String uuid = UUID.randomUUID().toString();
        String credentialId = "test-user-id";
        String role = "USER";
        String token = jwtUtil.createAccessJwt(uuid, credentialId, role);

        // when
        String extractedCredentialId = jwtUtil.getCredentialId(token);

        // then
        assertThat(extractedCredentialId).isEqualTo(credentialId);
    }

    @Test
    @DisplayName("생성된 토큰에서 role 추출")
    void getRole_FromToken_ReturnsCorrectValue() {
        // given
        String uuid = UUID.randomUUID().toString();
        String credentialId = "test-user-id";
        String role = "USER";
        String token = jwtUtil.createAccessJwt(uuid, credentialId, role);

        // when
        String extractedRole = jwtUtil.getRole(token);

        // then
        assertThat(extractedRole).isEqualTo(role);
    }

    @Test
    @DisplayName("생성된 토큰에서 UUID 추출")
    void getUUID_FromToken_ReturnsCorrectValue() {
        // given
        String uuid = UUID.randomUUID().toString();
        String credentialId = "test-user-id";
        String role = "USER";
        String token = jwtUtil.createAccessJwt(uuid, credentialId, role);

        // when
        String extractedUuid = jwtUtil.getUUID(token);

        // then
        assertThat(extractedUuid).isEqualTo(uuid);
    }

    @Test
    @DisplayName("새로 생성된 토큰은 만료되지 않음")
    void isAccessExpired_WithNewToken_ReturnsFalse() {
        // given
        String token = jwtUtil.createAccessJwt(
                UUID.randomUUID().toString(),
                "test-user-id",
                "USER"
        );

        // when
        Boolean isExpired = jwtUtil.isAccessExpired(token);

        // then
        assertThat(isExpired).isFalse();
    }

    @Test
    @DisplayName("Refresh Token 생성 시 유효한 토큰 반환")
    void createRefreshJwt_ReturnsValidToken() {
        // given
        String uuid = UUID.randomUUID().toString();
        String credentialId = "test-user-id";
        String role = "USER";

        // when
        String token = jwtUtil.createRefreshJwt(uuid, credentialId, role);

        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(jwtUtil.isRefreshExpired(token)).isFalse();
    }

    @Test
    @DisplayName("잘못된 토큰은 만료된 것으로 처리")
    void isAccessExpired_WithInvalidToken_ReturnsTrue() {
        // given
        String invalidToken = "invalid.token.here";

        // when
        Boolean isExpired = jwtUtil.isAccessExpired(invalidToken);

        // then
        assertThat(isExpired).isTrue();
    }
}
