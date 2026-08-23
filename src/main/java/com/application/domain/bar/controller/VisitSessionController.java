package com.application.domain.bar.controller;

import com.application.common.auth.dto.oauth2Dto.CustomOAuth2User;
import com.application.common.response.ResponseDto;
import com.application.domain.bar.dto.request.VisitSessionRequest;
import com.application.domain.bar.dto.response.VisitSessionDto;
import com.application.domain.bar.service.VisitSessionService;
import com.application.domain.bar.support.BarMemberResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 방문 세션 (T-11). 전부 JWT 필수.
 *
 * QR = 의도, GPS = 증명.
 *   QR 없음        → L1 (채팅만) ★ 구버전 앱 하위호환
 *   QR + GPS       → L2 (채팅 + 가격)
 *   mock=true      → L2 요청이어도 L1 로 강등
 *   반경 밖/위조QR → 403
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Bar", description = "매장 방문 세션 (QR/GPS)")
public class VisitSessionController {

    private final VisitSessionService visitSessionService;
    private final BarMemberResolver memberResolver;

    @Operation(summary = "방문 세션 발급", description = "qrPayload 생략 시 GPS만으로 L1(채팅만) 발급")
    @PostMapping("/api/v2/bars/{slug}/visit-session")
    public ResponseEntity<ResponseDto<VisitSessionDto>> issue(
            @PathVariable String slug,
            @RequestBody VisitSessionRequest request,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomOAuth2User principal) {

        Long memberId = memberResolver.resolveIdOrThrow(principal);
        VisitSessionDto dto = visitSessionService.issue(slug, memberId, request);
        return ResponseEntity.ok(ResponseDto.onSuccess("매장 인증 완료", dto));
    }

    @Operation(summary = "방문 세션 갱신", description = "좌표를 재증명한다. QR을 다시 주지 않으면 L1로 강등된다")
    @PostMapping("/api/v2/bars/{slug}/visit-session/renew")
    public ResponseEntity<ResponseDto<VisitSessionDto>> renew(
            @PathVariable String slug,
            @RequestBody VisitSessionRequest request,
            @RequestHeader(value = BarMenuController.SESSION_HEADER, required = false) String sessionToken,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomOAuth2User principal) {

        memberResolver.resolveIdOrThrow(principal);   // 로그인 상태 확인
        VisitSessionDto dto = visitSessionService.renew(slug, sessionToken, request);
        return ResponseEntity.ok(ResponseDto.onSuccess("매장 인증 갱신 완료", dto));
    }
}
