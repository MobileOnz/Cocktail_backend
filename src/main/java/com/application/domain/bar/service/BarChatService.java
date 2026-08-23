package com.application.domain.bar.service;

import com.application.common.exception.custom.CustomApiException;
import com.application.domain.bar.exception.BarAccessException;
import com.application.common.ratelimit.RateLimiter;
import com.application.common.sse.ChatEvent;
import com.application.common.sse.ChatEventBus;
import com.application.domain.bar.dto.response.ChatMessageDto;
import com.application.domain.bar.entity.Bar;
import com.application.domain.bar.entity.BarChatIdentity;
import com.application.domain.bar.entity.BarChatMessage;
import com.application.domain.bar.entity.BarVisitSession;
import com.application.domain.bar.repository.BarChatBlockRepository;
import com.application.domain.bar.repository.BarChatIdentityRepository;
import com.application.domain.bar.repository.BarChatMessageRepository;
import com.application.domain.bar.trust.TrustLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 익명 채팅.
 *
 * 입장 조건: 방문 세션의 trustLevel >= L1.
 *   L1 = GPS 반경 내(QR 없음)  → 채팅 가능. 현재 앱스토어 배포본이 이 경로다.
 *   L2 = QR + GPS             → 채팅 + 가격.
 * 즉 **QR 은 채팅의 전제가 아니다.** 가격에만 붙는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BarChatService {

    private static final int MAX_CONTENT = 500;
    private static final int BACKFILL_LIMIT = 200;

    private final BarService barService;
    private final VisitSessionService visitSessionService;
    private final AnonHandleService anonHandleService;
    private final BarChatMessageRepository messageRepository;
    private final BarChatIdentityRepository identityRepository;
    private final BarChatBlockRepository blockRepository;
    private final ChatEventBus chatEventBus;
    private final RateLimiter rateLimiter;

    /** 채팅 접근에 필요한 최소 신뢰등급. */
    private static final TrustLevel CHAT_MIN_TRUST = TrustLevel.L1;

    /** 세션 검증 + 등급 확인 + 신원 해석을 한 번에. */
    @Transactional
    public ChatContext enter(String slug, String sessionToken) {
        Bar bar = barService.getActiveBarOrThrow(slug);
        BarVisitSession session = visitSessionService.requireActiveSession(bar.getId(), sessionToken);

        if (!session.getTrustLevel().atLeast(CHAT_MIN_TRUST)) {
            throw BarAccessException.sessionRequired();
        }
        BarChatIdentity me = anonHandleService.resolve(session.getMemberId(), bar.getId());
        return new ChatContext(bar, session, me);
    }

    // ── 송신 ───────────────────────────────────────────────────────────

    @Transactional
    public ChatMessageDto send(String slug, String sessionToken, String rawContent) {
        ChatContext ctx = enter(slug, sessionToken);
        BarChatIdentity me = ctx.identity();

        if (me.isMuted(java.time.LocalDateTime.now())) {
            throw BarAccessException.muted();
        }

        String content = sanitize(rawContent);

        // 2초/1건, 60초/20건. 초과 시 429 대신 400 봉투(-1)로 나간다.
        if (!rateLimiter.tryAcquire("chat:burst:" + me.getAuthorRef(), 1, Duration.ofSeconds(2))
                || !rateLimiter.tryAcquire("chat:min:" + me.getAuthorRef(), 20, Duration.ofSeconds(60))) {
            throw BarAccessException.rateLimited("메시지를 너무 빠르게 보내고 있어요. 잠시 후 다시 시도해주세요.");
        }

        BarChatMessage saved = messageRepository.save(
                BarChatMessage.of(ctx.bar().getId(), me.getAuthorRef(), content));

        ChatMessageDto dto = ChatMessageDto.of(saved, me.getNickname(), null);
        chatEventBus.publish(ctx.bar().getId(), ChatEvent.message(saved.getId(), dto));
        return ChatMessageDto.of(saved, me.getNickname(), me.getAuthorRef());
    }

    // ── 히스토리 / SSE 백필 ─────────────────────────────────────────────

    /** Last-Event-ID 재개. lastEventId 가 null 이면 최근 N건. */
    @Transactional(readOnly = true)
    public List<ChatMessageDto> backfill(Long barId, String viewerRef, Long lastEventId) {
        List<BarChatMessage> rows;
        if (lastEventId == null) {
            rows = new ArrayList<>(messageRepository.findRecent(barId, PageRequest.of(0, 50)));
            java.util.Collections.reverse(rows);   // 오래된 순으로
        } else {
            rows = messageRepository.findAfterId(barId, lastEventId, PageRequest.of(0, BACKFILL_LIMIT));
        }
        return decorate(rows, viewerRef);
    }

    /** authorRef → nickname 해석 + 차단한 상대 필터링. N+1 없이 한 번에. */
    private List<ChatMessageDto> decorate(List<BarChatMessage> rows, String viewerRef) {
        if (rows.isEmpty()) return List.of();

        Set<String> refs = rows.stream().map(BarChatMessage::getAuthorRef).collect(Collectors.toSet());
        Map<String, String> nicknames = new HashMap<>();
        identityRepository.findAllById(refs)
                .forEach(i -> nicknames.put(i.getAuthorRef(), i.getNickname()));

        Set<String> blocked = viewerRef == null
                ? Set.of()
                : Set.copyOf(blockRepository.findBlockedRefs(viewerRef));

        return rows.stream()
                .filter(m -> !blocked.contains(m.getAuthorRef()))
                .map(m -> ChatMessageDto.of(m, nicknames.getOrDefault(m.getAuthorRef(), "알 수 없음"), viewerRef))
                .toList();
    }

    private String sanitize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new CustomApiException("메시지를 입력해주세요");
        }
        String s = raw.strip();
        if (s.length() > MAX_CONTENT) {
            throw new CustomApiException("메시지는 " + MAX_CONTENT + "자를 넘을 수 없어요");
        }
        // 링크 스팸 억제: URL 은 표시하되 클릭 유도를 막기 위해 스킴을 제거한다.
        return s.replaceAll("(?i)\\bhttps?://", "");
    }

    public record ChatContext(Bar bar, BarVisitSession session, BarChatIdentity identity) {}
}
