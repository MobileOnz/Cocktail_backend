package com.application.domain.bar.controller;

import com.application.common.response.ResponseDto;
import com.application.domain.bar.dto.response.BarMenuDto;
import com.application.domain.bar.service.BarMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 게이트된 메뉴판 (T-12).
 *
 * 세션 헤더가 없거나 신뢰등급이 L2 미만이면 응답에서 `price` 키 자체가 사라진다.
 * 클라이언트 마스킹이 아니다 — 서버가 값을 내려보내지 않는다.
 */
@RestController
@RequestMapping("/api/v2/bars")
@RequiredArgsConstructor
@Tag(name = "Bar", description = "칵테일바 메뉴판")
public class BarMenuController {

    /** plan_FINAL §4.3 의 헤더명. T-11 이 발급하는 방문 세션 토큰. */
    public static final String SESSION_HEADER = "X-Onz-Bar-Session";

    private final BarMenuService barMenuService;

    @Operation(summary = "메뉴판 조회",
               description = "X-Onz-Bar-Session 이 유효(L2)하면 price 포함, 아니면 price 키 자체를 생략하고 priceBand 만 제공")
    @GetMapping("/{slug}/menu")
    public ResponseEntity<ResponseDto<BarMenuDto>> menu(
            @PathVariable String slug,
            @RequestHeader(value = SESSION_HEADER, required = false) String sessionToken) {

        return ResponseEntity.ok(
                ResponseDto.onSuccess("메뉴판 조회 성공", barMenuService.getMenu(slug, sessionToken)));
    }
}
