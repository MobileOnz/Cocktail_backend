package com.application.common.auth.controller;

import com.application.common.auth.OAuth2Service;
import com.application.common.auth.dto.login.ReqSignupDto;
import com.application.common.auth.dto.login.ReqSocialLoginDto;
import com.application.common.auth.dto.login.ResSocialLoginDto;
import com.application.common.auth.dto.login.ResTokenDto;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(
            summary = "소셜 로그인 요청",
            description = """
                    소셜 플랫폼(카카오, 네이버, 애플, 구글)에서 받은 인증 코드 또는 액세스 토큰을 이용해 로그인합니다.

                    ## 응답 유형
                    - **기존 회원**: JWT 토큰 쌍(accessToken, refreshToken)을 반환 (type: "token")
                    - **신규 회원**: 회원가입을 위한 임시 코드를 반환 (type: "signup")

                    ## 사용 방식

                    ### 1️⃣ 모바일 앱 방식 (권장)
                    React Native, Flutter 등 모바일 앱에서 네이버/카카오/구글 SDK를 사용하는 경우
                    ```json
                    {
                      "provider": "naver",
                      "accessToken": "AAAANvMf...SDK에서_받은_토큰"
                    }
                    ```

                    ### 2️⃣ 웹 OAuth 방식
                    웹에서 OAuth 리다이렉트 콜백을 받는 경우

                    **네이버 (state 필요):**
                    ```json
                    {
                      "provider": "naver",
                      "code": "authorization_code",
                      "state": "csrf_state_value"
                    }
                    ```

                    **카카오/구글 (state 불필요):**
                    ```json
                    {
                      "provider": "kakao",
                      "code": "authorization_code"
                    }
                    ```
                    """
    )
    @PostMapping("/social-login")
    public ResponseEntity<ResSocialLoginDto> socialLogin(@RequestBody ReqSocialLoginDto dto) {
        // Service의 socialLogin 호출 -> DB 조회 -> 로그인 또는 가입대기 코드 반환
        return ResponseEntity.ok(oAuth2Service.socialLogin(dto));
    }

    /**
     * 회원가입 : 약관 동의 후 최종 가입
     */
    @Operation(
            summary = "회원가입 완료 (약관 동의)",
            description = "소셜 로그인 시 신규 회원으로 판별되어 받은 가입 대기 코드와 약관 동의 정보를 제출하여 최종 가입합니다."
    )
    @PostMapping("/signup")
    public ResponseEntity<ResSocialLoginDto> signup(@RequestBody ReqSignupDto dto) {
        // Service의 signup 호출 -> DB 저장 -> 토큰 발급
        return ResponseEntity.ok(oAuth2Service.signup(dto));
    }

    /**
     * 로그아웃
     */
    @Operation(
            summary = "로그아웃",
            description = "현재 사용 중인 Access Token을 블랙리스트에 등록하고, 서버의 Refresh Token을 삭제합니다."
    )
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String token) {
        // Service의 logout 호출 -> 토큰 블랙리스트 처리
        oAuth2Service.logout(token);
        return ResponseEntity.ok().build();
    }

    /**
     * 토큰 재발급 (Refresh Token)
     */
    @Operation(
            summary = "토큰 재발급 (Reissue)",
            description = "만료된 Access Token 대신 Refresh Token을 사용하여 새로운 토큰 쌍을 발급받습니다."
    )
    @PostMapping("/reissue")
    public ResponseEntity<ResTokenDto> reissue(@RequestHeader("RefreshToken") String refreshToken){
        return ResponseEntity.ok(oAuth2Service.reissueRefreshToken(refreshToken));
    }
}
