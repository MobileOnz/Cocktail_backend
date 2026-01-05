package com.application.test;

import com.application.common.auth.jwt.JWTUtil;
import com.application.common.response.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Tag(name = "테스트용 API", description = "개발 환경 전용 테스트 API")
public class TestTokenController {

    private final JWTUtil jwtUtil;

    @Operation(
            summary = "[테스트] JWT 토큰 생성",
            description = "테스트용 JWT Access Token을 생성합니다. credentialId를 전달하면 해당 사용자의 토큰을 생성합니다."
    )
    @SecurityRequirements
    @PostMapping("/token")
    public ResponseEntity<ResponseDto<TokenResponse>> generateTestToken(
            @RequestBody(required = false) TokenRequest request
    ) {
        String credentialId = (request != null && request.getCredentialId() != null)
                ? request.getCredentialId()
                : "test-user-123";

        String uuid = UUID.randomUUID().toString();
        String role = "ROLE_USER";

        String accessToken = jwtUtil.createAccessJwt(uuid, credentialId, role);
        String refreshToken = jwtUtil.createRefreshJwt(uuid, credentialId, role);

        TokenResponse response = new TokenResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setCredentialId(credentialId);
        response.setMessage("Authorization 헤더에 'Bearer " + accessToken + "' 형식으로 추가하세요");

        return new ResponseEntity<>(
                ResponseDto.onSuccess("테스트 토큰 생성 성공", response),
                HttpStatus.OK
        );
    }

    @Getter
    @Setter
    public static class TokenRequest {
        private String credentialId;
    }

    @Getter
    @Setter
    public static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private String credentialId;
        private String message;
    }
}
