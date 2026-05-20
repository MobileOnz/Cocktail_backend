package com.application.common.auth.controller;

import com.application.common.auth.jwt.JWTUtil;
import com.application.common.response.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/test/auth")
@RequiredArgsConstructor
@Tag(name = "테스트 인증 API", description = "개발/테스트용 JWT 토큰 생성 API")
public class TestAuthController {

    private final JWTUtil jwtUtil;

    /**
     * 테스트용 JWT 토큰 생성
     * GET /api/v2/test/auth/token?credentialId=test_user_001
     */
    @Operation(
            summary = "테스트용 JWT 토큰 생성",
            description = "개발/테스트 환경에서 사용할 JWT 토큰을 생성합니다. " +
                    "credentialId를 입력하면 해당 사용자의 Access Token과 Refresh Token을 반환합니다."
    )
    @GetMapping("/token")
    public ResponseEntity<ResponseDto<Map<String, String>>> generateTestToken(
            @RequestParam(defaultValue = "test_user_001") String credentialId
    ) {
        // UUID 생성
        String uuid = UUID.randomUUID().toString();

        // 기본 role은 USER
        String role = "ROLE_USER";

        // Access Token 생성
        String accessToken = jwtUtil.createAccessJwt(uuid, credentialId, role);

        // Refresh Token 생성
        String refreshToken = jwtUtil.createRefreshJwt(uuid, credentialId, role);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        tokens.put("credentialId", credentialId);
        tokens.put("uuid", uuid);
        tokens.put("role", role);

        return new ResponseEntity<>(
                ResponseDto.onSuccess("테스트 토큰 생성 성공", tokens),
                HttpStatus.OK
        );
    }
}