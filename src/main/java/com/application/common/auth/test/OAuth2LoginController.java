package com.application.common.auth.test;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/api/v2/auth")
public class OAuth2LoginController {
    private final OAuth2LoginUrlService loginUrlService;

    public OAuth2LoginController(OAuth2LoginUrlService loginUrlService) {
        this.loginUrlService = loginUrlService;
    }

    // 네이버 로그인 URL 반환
    @GetMapping("/naver/login-url")
    public ResponseEntity<Map<String, String>> getNaverLoginUrl() {
        return ResponseEntity.ok(Map.of("loginUrl", loginUrlService.getNaverLoginUrl()));
    }

    // 구글 로그인 URL 반환
    @GetMapping("/google/login-url")
    public ResponseEntity<Map<String, String>> getGoogleLoginUrl() {
        return ResponseEntity.ok(Map.of("loginUrl", loginUrlService.getGoogleLoginUrl()));
    }

    // 카카오 로그인 URL 반환
    @GetMapping("/kakao/login-url")
    public ResponseEntity<Map<String, String>> getKakaoLoginUrl() {
        return ResponseEntity.ok(Map.of("loginUrl", loginUrlService.getKakaoLoginUrl()));
    }
}
