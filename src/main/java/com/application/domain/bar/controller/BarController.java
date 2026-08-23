package com.application.domain.bar.controller;

import com.application.common.auth.dto.oauth2Dto.CustomOAuth2User;
import com.application.common.response.ResponseDto;
import com.application.domain.bar.dto.response.BarDetailDto;
import com.application.domain.bar.dto.response.BarListItemDto;
import com.application.domain.bar.service.BarService;
import com.application.domain.bar.support.BarMemberResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 바 조회 (PUBLIC / OPTIONAL 인증).
 *
 * 경로는 Node(참조 구현) 계약을 그대로 이주했다:
 *   GET /bars, /bars/nearby, /bars/{slug}
 * 정적 경로(/nearby)를 파라미터 경로(/{slug})보다 먼저 선언한다.
 */
@RestController
@RequestMapping("/api/v2/bars")
@RequiredArgsConstructor
@Tag(name = "Bar", description = "칵테일바 조회")
public class BarController {

    private final BarService barService;
    private final BarMemberResolver memberResolver;

    @Operation(summary = "바 목록", description = "sort=curated|recent|distance. distance 는 lat/lng 필수")
    @GetMapping
    public ResponseEntity<ResponseDto<List<BarListItemDto>>> list(
            @RequestParam(required = false, defaultValue = "curated") String sort,
            @RequestParam(required = false, defaultValue = "30") Integer limit,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng) {

        return ResponseEntity.ok(
                ResponseDto.onSuccess("바 목록 조회 성공", barService.getBars(sort, limit, lat, lng)));
    }

    @Operation(summary = "주변 바", description = "bounding-box 선필터 + Haversine 정렬. PostGIS 미사용")
    @GetMapping("/nearby")
    public ResponseEntity<ResponseDto<List<BarListItemDto>>> nearby(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(required = false, defaultValue = "3000") Double radiusM,
            @RequestParam(required = false, defaultValue = "30") Integer limit) {

        return ResponseEntity.ok(
                ResponseDto.onSuccess("주변 바 조회 성공", barService.nearby(lat, lng, radiusM, limit)));
    }

    @Operation(summary = "바 상세", description = "로그인 시 isVisited 반영")
    @GetMapping("/{slug}")
    public ResponseEntity<ResponseDto<BarDetailDto>> detail(
            @PathVariable String slug,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomOAuth2User principal) {

        Long memberId = memberResolver.resolveIdOrNull(principal);
        return ResponseEntity.ok(
                ResponseDto.onSuccess("바 상세 조회 성공", barService.getBySlug(slug, memberId)));
    }
}
