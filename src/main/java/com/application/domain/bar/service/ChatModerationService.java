package com.application.domain.bar.service;

import com.application.common.exception.custom.CustomApiException;
import com.application.common.sse.ChatEvent;
import com.application.common.sse.ChatEventBus;
import com.application.domain.bar.entity.BarChatBlock;
import com.application.domain.bar.entity.BarChatIdentity;
import com.application.domain.bar.entity.BarChatMessage;
import com.application.domain.bar.repository.BarChatBlockRepository;
import com.application.domain.bar.repository.BarChatIdentityRepository;
import com.application.domain.bar.repository.BarChatMessageRepository;
import com.application.domain.bar.repository.BarChatReportRepository;
import com.application.domain.bar.entity.BarChatReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 신고 / 차단 / 뮤트 (T-14).
 *
 * 임계치는 **서로 다른 신고자 수**로 센다. UNIQUE(message_id, reporter_ref) 가
 * 한 사람의 반복 신고를 막고, COUNT(DISTINCT reporter_ref) 가 자작 신고폭탄을 막는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatModerationService {

    private static final Set<String> VALID_REASONS = Set.of("SPAM", "ABUSE", "SEXUAL", "ADVERTISE", "OTHER");

    /** 이 수만큼 서로 다른 사람이 신고하면 자동 블라인드. */
    @Value("${onz.chat.report-threshold:3}")
    private int reportThreshold;

    /** 자동 블라인드 시 작성자에게 부과되는 뮤트 시간. */
    @Value("${onz.chat.auto-mute-hours:24}")
    private long autoMuteHours;

    private final BarChatService barChatService;
    private final BarChatMessageRepository messageRepository;
    private final BarChatReportRepository reportRepository;
    private final BarChatBlockRepository blockRepository;
    private final BarChatIdentityRepository identityRepository;
    private final ChatEventBus chatEventBus;

    /** 신고. 임계치 도달 시 블라인드 + SSE 'hidden' 브로드캐스트. */
    @Transactional
    public void report(String slug, String sessionToken, Long messageId, String reason, String detail) {
        var ctx = barChatService.enter(slug, sessionToken);
        String reporterRef = ctx.identity().getAuthorRef();

        String normalized = reason == null ? "OTHER" : reason.toUpperCase();
        if (!VALID_REASONS.contains(normalized)) {
            throw new CustomApiException("올바르지 않은 신고 사유입니다");
        }

        BarChatMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new CustomApiException("존재하지 않는 메시지입니다"));

        if (!message.getBarId().equals(ctx.bar().getId())) {
            throw new CustomApiException("존재하지 않는 메시지입니다");
        }
        if (message.getAuthorRef().equals(reporterRef)) {
            throw new CustomApiException("자신의 메시지는 신고할 수 없습니다");
        }
        if (reportRepository.existsByMessageIdAndReporterRef(messageId, reporterRef)) {
            throw new CustomApiException("이미 신고한 메시지입니다");
        }

        reportRepository.save(BarChatReport.of(messageId, reporterRef, normalized, detail));

        long distinct = reportRepository.countDistinctReporters(messageId);
        boolean wasVisible = message.isVisible();
        message.applyReport((int) distinct, reportThreshold);

        if (wasVisible && !message.isVisible()) {
            // 임시조치: 즉시 블라인드하고 24시간 내 운영자가 검토한다(정보통신망법 §44-2).
            identityRepository.findById(message.getAuthorRef())
                    .ifPresent(author -> author.mute(LocalDateTime.now().plusHours(autoMuteHours)));

            chatEventBus.publish(message.getBarId(), ChatEvent.hidden(message.getId()));
            log.info("자동 블라인드: messageId={} distinctReporters={}", messageId, distinct);
        }
    }

    /** 차단. 재설치해도 유지되도록 서버에 저장한다. */
    @Transactional
    public void block(String slug, String sessionToken, String blockedRef) {
        var ctx = barChatService.enter(slug, sessionToken);
        String blockerRef = ctx.identity().getAuthorRef();

        if (blockerRef.equals(blockedRef)) {
            throw new CustomApiException("자기 자신은 차단할 수 없습니다");
        }
        if (blockRepository.existsByBlockerRefAndBlockedRef(blockerRef, blockedRef)) {
            return;   // 멱등
        }
        blockRepository.save(BarChatBlock.of(blockerRef, blockedRef));
    }

    /** 운영자 제재용. author_ref → member_id 역추적은 identity 테이블을 통해서만 가능하다. */
    @Transactional
    public void muteByAuthorRef(String authorRef, long hours) {
        BarChatIdentity identity = identityRepository.findById(authorRef)
                .orElseThrow(() -> new CustomApiException("이미 파기된 신원입니다(30일 경과)"));
        identity.mute(LocalDateTime.now().plusHours(hours));
    }
}
