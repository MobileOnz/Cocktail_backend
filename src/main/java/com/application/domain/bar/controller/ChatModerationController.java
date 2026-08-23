package com.application.domain.bar.controller;

import com.application.common.response.ResponseDto;
import com.application.domain.bar.dto.request.ReportRequest;
import com.application.domain.bar.service.ChatModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 신고 / 차단 (T-14). 세션(L1 이상) 필수. */
@RestController
@RequiredArgsConstructor
@Tag(name = "BarChat", description = "채팅 모더레이션")
public class ChatModerationController {

    private final ChatModerationService moderationService;

    @Operation(summary = "메시지 신고", description = "서로 다른 3명이 신고하면 자동 블라인드 + SSE 'hidden' 브로드캐스트")
    @PostMapping("/api/v2/bars/{slug}/chat/messages/{messageId}/report")
    public ResponseEntity<ResponseDto<Void>> report(
            @PathVariable String slug,
            @PathVariable Long messageId,
            @RequestHeader(value = BarMenuController.SESSION_HEADER, required = false) String sessionToken,
            @RequestBody ReportRequest request) {

        moderationService.report(slug, sessionToken, messageId, request.reason(), request.detail());
        return ResponseEntity.ok(ResponseDto.onSuccess("신고가 접수됐어요", null));
    }

    @Operation(summary = "작성자 차단", description = "authorRef 기준. 재설치해도 유지된다")
    @PostMapping("/api/v2/bars/{slug}/chat/identities/{authorRef}/block")
    public ResponseEntity<ResponseDto<Void>> block(
            @PathVariable String slug,
            @PathVariable String authorRef,
            @RequestHeader(value = BarMenuController.SESSION_HEADER, required = false) String sessionToken) {

        moderationService.block(slug, sessionToken, authorRef);
        return ResponseEntity.ok(ResponseDto.onSuccess("차단했어요", null));
    }
}
