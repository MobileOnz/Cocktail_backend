package com.application.domain.bar.controller;

import com.application.common.response.ResponseDto;
import com.application.common.sse.ChatEvent;
import com.application.common.sse.ChatEventBus;
import com.application.common.sse.SseConnectionRegistry;
import com.application.domain.bar.dto.request.ChatMessageRequest;
import com.application.domain.bar.dto.response.ChatMessageDto;
import com.application.domain.bar.service.BarChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

/**
 * 익명 채팅 (T-13). 입장 조건: 방문 세션 trustLevel >= L1.
 *
 * SSE 는 Servlet async 라 요청 스레드를 잡지 않는다. 연결 상한/하트비트/타임아웃은
 * {@link SseConnectionRegistry} 가 관리한다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "BarChat", description = "매장 익명 채팅")
public class BarChatController {

    private final BarChatService barChatService;
    private final ChatEventBus chatEventBus;
    private final SseConnectionRegistry registry;

    @Operation(summary = "채팅 수신 (SSE)", description = "Last-Event-ID 헤더로 재개 가능")
    @GetMapping(value = "/api/v2/bars/{slug}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @PathVariable String slug,
            @RequestHeader(value = BarMenuController.SESSION_HEADER, required = false) String sessionToken,
            @RequestHeader(value = "Last-Event-ID", required = false) Long lastEventId) {

        // 세션/등급 검증. 실패하면 SSE 를 열기 전에 예외 → {code,msg,data} 봉투로 나간다.
        BarChatService.ChatContext ctx = barChatService.enter(slug, sessionToken);
        Long barId = ctx.bar().getId();
        String viewerRef = ctx.identity().getAuthorRef();

        SseEmitter emitter = new SseEmitter(registry.timeoutMs());

        // 구독을 먼저 걸어야 백필과 라이브 사이의 이벤트를 놓치지 않는다.
        AutoCloseable subscription = chatEventBus.subscribe(barId, event -> {
            try {
                send(emitter, event);
            } catch (IOException | IllegalStateException e) {
                emitter.completeWithError(e);   // onError → registry 가 정리
            }
        });

        // 상한 초과면 여기서 CustomApiException → 구독은 register 내부에서 해제된다.
        registry.register(emitter, subscription);

        try {
            List<ChatMessageDto> backfill = barChatService.backfill(barId, viewerRef, lastEventId);
            for (ChatMessageDto m : backfill) {
                send(emitter, ChatEvent.message(m.id(), m));
            }
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    private void send(SseEmitter emitter, ChatEvent event) throws IOException {
        SseEmitter.SseEventBuilder builder = SseEmitter.event()
                .name(event.type())
                .data(event.payload(), MediaType.APPLICATION_JSON);

        // id: 가 있어야 클라이언트가 Last-Event-ID 로 재개할 수 있다.
        if (event.eventId() != null) {
            builder = builder.id(String.valueOf(event.eventId()));
        }
        emitter.send(builder);
    }

    @Operation(summary = "채팅 송신")
    @PostMapping("/api/v2/bars/{slug}/chat/messages")
    public ResponseEntity<ResponseDto<ChatMessageDto>> send(
            @PathVariable String slug,
            @RequestHeader(value = BarMenuController.SESSION_HEADER, required = false) String sessionToken,
            @RequestBody ChatMessageRequest request) {

        ChatMessageDto dto = barChatService.send(slug, sessionToken, request.content());
        return ResponseEntity.ok(ResponseDto.onSuccess("메시지 전송 성공", dto));
    }

    @Operation(summary = "채팅 히스토리", description = "SSE 를 쓰지 못하는 클라이언트의 폴백")
    @GetMapping("/api/v2/bars/{slug}/chat/messages")
    public ResponseEntity<ResponseDto<List<ChatMessageDto>>> messages(
            @PathVariable String slug,
            @RequestHeader(value = BarMenuController.SESSION_HEADER, required = false) String sessionToken,
            @RequestParam(required = false) Long afterId) {

        BarChatService.ChatContext ctx = barChatService.enter(slug, sessionToken);
        List<ChatMessageDto> list = barChatService.backfill(
                ctx.bar().getId(), ctx.identity().getAuthorRef(), afterId);
        return ResponseEntity.ok(ResponseDto.onSuccess("메시지 조회 성공", list));
    }
}
