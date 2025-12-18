package com.application.common.auth.test;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/api/v2/auth")
@Tag(name = "소셜 로그인 URL", description = "테스트/개발용 소셜 로그인 URL 조회 API")
public class OAuth2LoginController {
    private final OAuth2LoginUrlService loginUrlService;

    public OAuth2LoginController(OAuth2LoginUrlService loginUrlService) {
        this.loginUrlService = loginUrlService;
    }

    @Operation(
            summary = "네이버 로그인 URL 조회",
            description = "네이버 OAuth2 인증을 위한 로그인 URL을 반환합니다. " +
                    "반환된 URL로 리다이렉트하면 사용자는 네이버 로그인 페이지로 이동합니다."
    )
    @GetMapping("/naver/login-url")
    public ResponseEntity<Map<String, String>> getNaverLoginUrl() {
        return ResponseEntity.ok(Map.of("loginUrl", loginUrlService.getNaverLoginUrl()));
    }

    @Operation(
            summary = "구글 로그인 URL 조회",
            description = "구글 OAuth2 인증을 위한 로그인 URL을 반환합니다. " +
                    "반환된 URL로 리다이렉트하면 사용자는 구글 로그인 페이지로 이동합니다."
    )
    @GetMapping("/google/login-url")
    public ResponseEntity<Map<String, String>> getGoogleLoginUrl() {
        return ResponseEntity.ok(Map.of("loginUrl", loginUrlService.getGoogleLoginUrl()));
    }

    @Operation(
            summary = "카카오 로그인 URL 조회",
            description = "카카오 OAuth2 인증을 위한 로그인 URL을 반환합니다. " +
                    "반환된 URL로 리다이렉트하면 사용자는 카카오 로그인 페이지로 이동합니다."
    )
    @GetMapping("/kakao/login-url")
    public ResponseEntity<Map<String, String>> getKakaoLoginUrl() {
        return ResponseEntity.ok(Map.of("loginUrl", loginUrlService.getKakaoLoginUrl()));
    }
}
