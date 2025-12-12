package com.application.common.auth.controller;

import com.application.common.auth.OAuth2Service;
import com.application.common.auth.dto.login.ReqSignupDto;
import com.application.common.auth.dto.login.ReqSocialLoginDto;
import com.application.common.auth.dto.login.ResSocialLoginDto;
import com.application.common.auth.dto.login.ResTokenDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OAuth2Service oAuth2Service;

    /**
     * 소셜 로그인 요청 : (OAuth2CodeController가 여기로 요청을 보냄)
     */
    @PostMapping("/social-login")
    public ResponseEntity<ResSocialLoginDto> socialLogin(@RequestBody ReqSocialLoginDto dto) {
        // Service의 socialLogin 호출 -> DB 조회 -> 로그인 또는 가입대기 코드 반환
        return ResponseEntity.ok(oAuth2Service.socialLogin(dto));
    }

    /**
     * 회원가입 : 약관 동의 후 최종 가입
     */
    @PostMapping("/signup")
    public ResponseEntity<ResSocialLoginDto> signup(@RequestBody ReqSignupDto dto) {
        // Service의 signup 호출 -> DB 저장 -> 토큰 발급
        return ResponseEntity.ok(oAuth2Service.signup(dto));
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        // Service의 logout 호출 -> 토큰 블랙리스트 처리
        oAuth2Service.logout(token);
        return ResponseEntity.ok().build();
    }

    /**
     * 토큰 재발급 (Refresh Token)
     */
    @PostMapping("/reissue")
    public ResponseEntity<ResTokenDto> reissue(@RequestHeader("RefreshToken") String refreshToken){
        return ResponseEntity.ok(oAuth2Service.reissueRefreshToken(refreshToken));
    }
}
