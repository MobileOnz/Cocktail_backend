package com.application.domain.bar.controller;

import com.application.common.auth.dto.oauth2Dto.CustomOAuth2User;
import com.application.common.response.ResponseDto;
import com.application.domain.bar.dto.response.VisitPageDto;
import com.application.domain.bar.service.BarVisitService;
import com.application.domain.bar.support.BarMemberResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 방문 체크 토글 + 내 방문 목록 (둘 다 JWT 필수).
 *
 * ⚠️ JWTFilter.REQUIRE_AUTH_PATH_PATTERNS 등록이 필요하다. 그 파일은 s3 소유이므로
 *    여기서 고치지 않고 done_T10_T12.md 에 요청으로 남긴다.
 *    등록 전까지는 BarMemberResolver 가 401(로그인 필요)을 던진다 — 조용히 통과하지 않는다.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Bar", description = "칵테일바 방문")
public class BarVisitController {

    private final BarVisitService barVisitService;
    private final BarMemberResolver memberResolver;

    @Operation(summary = "방문 체크")
    @PostMapping("/api/v2/bars/{slug}/visit")
    public ResponseEntity<ResponseDto<Map<String, Boolean>>> check(
            @PathVariable String slug,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomOAuth2User principal) {

        Long memberId = memberResolver.resolveIdOrThrow(principal);
        boolean visited = barVisitService.check(memberId, slug);
        return ResponseEntity.ok(ResponseDto.onSuccess("방문 체크 완료", Map.of("isVisited", visited)));
    }

    @Operation(summary = "방문 해제")
    @DeleteMapping("/api/v2/bars/{slug}/visit")
    public ResponseEntity<ResponseDto<Map<String, Boolean>>> uncheck(
            @PathVariable String slug,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomOAuth2User principal) {

        Long memberId = memberResolver.resolveIdOrThrow(principal);
        boolean visited = barVisitService.uncheck(memberId, slug);
        return ResponseEntity.ok(ResponseDto.onSuccess("방문 해제 완료", Map.of("isVisited", visited)));
    }

    @Operation(summary = "내가 방문한 바", description = "id DESC 커서 페이징. nextCursor 가 null 이면 끝")
    @GetMapping("/api/v2/me/visits")
    public ResponseEntity<ResponseDto<VisitPageDto>> myVisits(
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomOAuth2User principal) {

        Long memberId = memberResolver.resolveIdOrThrow(principal);
        return ResponseEntity.ok(
                ResponseDto.onSuccess("방문한 바 조회 성공", barVisitService.myVisits(memberId, cursor, limit)));
    }
}
