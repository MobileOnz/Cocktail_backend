package com.application.common.auth.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class OAuth2PropertiesValue {
    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;

    @Value("${spring.security.oauth2.client.registration.naver.redirect-uri}")
    private String naverRedirectUri;

    @Value("${spring.security.oauth2.client.provider.naver.token-uri}")
    private String naverTokenUri;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String googleRedirectUri;

    @Value("${spring.security.oauth2.client.provider.google.token-uri}")
    private String googleTokenUri;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${spring.security.oauth2.client.provider.kakao.token-uri}")
    private String kakaoTokenUri;

//    @Value("${spring.security.oauth2.client.secret.key}")
    @Value("${APPLE_PRIVATE_KEY}")
    private String appleKey;

//    @Value("${spring.security.oauth2.client.id}")
    @Value("${APPLE_CLIENT_ID}")
    private String appleClientId;

//    @Value("${spring.security.oauth2.client.key.id}")
    @Value("${APPLE_KEY_ID}")
    private String appleKeyId;

//    @Value("${spring.security.oauth2.client.team.id}")
    @Value("${APPLE_TEAM_ID}")
    private String appleTeamId;

    @Value("${APPLE_AUDIENCE}")
    private String appleAudience;

    @Value("${APPLE_PUBLIC_KEY_URL}")
    private String applePublicKeyUrl;

    @Value("${APPLE_TOKEN_URL}")
    private String appleTokenUrl;
}
