package com.application.domain.inquiry.controller;

import com.application.common.auth.dto.oauth2Dto.CustomOAuth2User;
import com.application.common.response.ResponseDto;
import com.application.domain.inquiry.dto.InquiryCreateRequestDto;
import com.application.domain.inquiry.service.InquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 1:1 문의 접수 (앱 → 서버).
 *
 * <p>앱에는 문의 화면이 있는데 이 엔드포인트가 없어서 '문의 보내기'가 항상 실패했다.
 * 어드민의 문의 목록에 시드 3건만 있던 이유다.</p>
 *
 * <p>인증은 <b>선택</b>이다(정책 OPTIONAL). 로그인이 안 돼서 문의하는 경우가 실제로 있으므로
 * 토큰을 요구하면 정작 가장 도움이 필요한 사람이 못 보낸다.</p>
 */
@Tag(name = "1:1 문의", description = "앱에서 보내는 문의 접수")
@RestController
@RequestMapping("/api/v2/inquiry")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @Operation(summary = "문의 접수",
            description = "로그인 없이도 보낼 수 있다. 비로그인은 연락처 또는 기기 식별자 중 하나가 필요하다.")
    @PostMapping
    public ResponseEntity<ResponseDto<Map<String, Long>>> create(
            @Valid @RequestBody InquiryCreateRequestDto request,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomOAuth2User user,
            HttpServletRequest http
    ) {
        Long id = inquiryService.create(request, user, rateLimitKey(request, user, http));
        return new ResponseEntity<>(
                ResponseDto.onSuccess("문의가 접수되었습니다.", Map.of("inquiryId", id)),
                HttpStatus.CREATED);
    }

    /**
     * 레이트리밋 기준 키.
     *
     * <p>회원 → 기기ID → IP 순으로 고른다. IP 는 최후의 수단이다 — 같은 공유기를 쓰는 사람들이
     * 한도를 나눠 쓰게 되므로 식별자가 있으면 그쪽을 먼저 쓴다.</p>
     *
     * <p>nginx 가 앞에 있어 remoteAddr 은 프록시 주소다. X-Forwarded-For 의 첫 값이 원 클라이언트다
     * (이 값은 위조될 수 있지만, 여기선 과금·인가가 아니라 남용 완화 용도라 그 정도로 충분하다).</p>
     */
    private String rateLimitKey(InquiryCreateRequestDto req, CustomOAuth2User user, HttpServletRequest http) {
        if (user != null && user.getCredentialId() != null) {
            return "m:" + user.getCredentialId();
        }
        if (req.deviceNumber() != null && !req.deviceNumber().isBlank()) {
            return "d:" + req.deviceNumber().trim();
        }
        String forwarded = http.getHeader("X-Forwarded-For");
        String ip = (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : http.getRemoteAddr();
        return "ip:" + ip;
    }
}
