package com.application.common.auth.strategy;

import com.application.common.auth.config.OAuth2PropertiesValue;
import com.application.common.exception.custom.CustomApiException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * F-Apple-1 회귀 방지.
 *
 * <p>이전에는 id_token의 서명만 확인하고 aud(발급 대상 앱)/iss(발급자)는 검증하지 않았다.
 * 애플은 모든 릴라잉 파티에 동일한 JWKS로 서명하므로, 이 검증이 없으면 "다른 애플
 * 앱/서비스용으로 정상 발급된" 토큰을 이 백엔드에 그대로 재사용(replay)해 로그인할 수
 * 있었다. 실제 애플 JWKS는 호출할 수 없으므로, 테스트에서 자체 RSA 키쌍으로 서명한
 * id_token과 그 공개키를 담은 가짜 JWKS 응답을 RestTemplate mock으로 대신한다.</p>
 */
class AppleLoginStrategyTest {

    private static final String KID = "test-kid";
    private static final String EXPECTED_AUD = "com.onz.cocktail"; // 이 서버가 기대하는 APPLE_CLIENT_ID
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    private KeyPair keyPair;
    private AppleLoginStrategy strategy;

    @BeforeEach
    void setUp() throws Exception {
        keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        OAuth2PropertiesValue properties = new OAuth2PropertiesValue();
        ReflectionTestUtils.setField(properties, "appleClientId", EXPECTED_AUD);
        ReflectionTestUtils.setField(properties, "applePublicKeyUrl", "https://appleid.apple.com/auth/keys");

        RestTemplate restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(fakeJwks()));

        strategy = new AppleLoginStrategy(restTemplate, properties);
    }

    @Test
    @DisplayName("우리 앱(aud)을 대상으로, 애플(iss)이 발급한 토큰은 그대로 통과한다")
    void acceptsTokenWithCorrectAudienceAndIssuer() {
        String idToken = signedToken(EXPECTED_AUD, APPLE_ISSUER);

        Map<String, Object> userInfo = strategy.getUserInfo(idToken);

        assertThat(userInfo.get("sub")).isEqualTo("apple-user-sub");
    }

    @Test
    @DisplayName("다른 앱용(aud 불일치)으로 발급된 토큰은 거절된다 — replay 방지")
    void rejectsTokenWithWrongAudience() {
        String idToken = signedToken("some-other-app.bundle.id", APPLE_ISSUER);

        assertThatThrownBy(() -> strategy.getUserInfo(idToken))
                .isInstanceOfAny(JwtException.class, CustomApiException.class);
    }

    @Test
    @DisplayName("발급자(iss)가 애플이 아닌 토큰은 거절된다")
    void rejectsTokenWithWrongIssuer() {
        String idToken = signedToken(EXPECTED_AUD, "https://not-apple.example.com");

        assertThatThrownBy(() -> strategy.getUserInfo(idToken))
                .isInstanceOfAny(JwtException.class, CustomApiException.class);
    }

    private String signedToken(String aud, String iss) {
        Instant now = Instant.now();
        return Jwts.builder()
                .header().add("kid", KID).and()
                .issuer(iss)
                .audience().add(aud).and()
                .subject("apple-user-sub")
                .claim("email", "user@example.com")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    private String fakeJwks() {
        RSAPublicKey pub = (RSAPublicKey) keyPair.getPublic();
        String n = Base64.getUrlEncoder().withoutPadding().encodeToString(pub.getModulus().toByteArray());
        String e = Base64.getUrlEncoder().withoutPadding().encodeToString(pub.getPublicExponent().toByteArray());
        return "{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"" + KID + "\",\"use\":\"sig\",\"alg\":\"RS256\",\"n\":\"" + n + "\",\"e\":\"" + e + "\"}]}";
    }
}
