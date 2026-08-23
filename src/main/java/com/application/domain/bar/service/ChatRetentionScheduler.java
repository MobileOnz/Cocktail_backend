package com.application.domain.bar.service;

import com.application.domain.bar.repository.BarChatIdentityRepository;
import com.application.domain.bar.repository.BarChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 보존기간 배치 (T-14).
 *
 *   메시지  7일  → 물리 삭제. 06:00 KST 리셋은 조회 필터일 뿐이라, 이것이 없으면 영구 보관된다.
 *   identity 30일 → 파기. 이 시점 이후 메시지는 **수학적으로 익명**이 된다
 *                   (HMAC 역산 불가 + 매핑표 소멸).
 *
 * 매일 04:00 KST 실행. 채팅이 가장 한산한 시각이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRetentionScheduler {

    @Value("${onz.chat.message-retention-days:7}")
    private long messageRetentionDays;

    @Value("${onz.chat.identity-retention-days:30}")
    private long identityRetentionDays;

    private final BarChatMessageRepository messageRepository;
    private final BarChatIdentityRepository identityRepository;

    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void purge() {
        LocalDateTime now = LocalDateTime.now();

        int messages = messageRepository.deleteOlderThan(now.minusDays(messageRetentionDays));
        // 메시지를 먼저 지운 뒤 신원을 지운다(신고 조사 창구를 마지막까지 남긴다).
        int identities = identityRepository.deleteOlderThan(now.minusDays(identityRetentionDays));

        log.info("채팅 보존기간 배치: 메시지 {}건, 신원 {}건 삭제", messages, identities);
    }

    /** 테스트/운영 수동 호출용. */
    @Transactional
    public int purgeMessagesOlderThan(LocalDateTime cutoff) {
        return messageRepository.deleteOlderThan(cutoff);
    }
}
