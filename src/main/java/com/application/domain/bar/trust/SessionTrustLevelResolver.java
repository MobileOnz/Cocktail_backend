package com.application.domain.bar.trust;

import com.application.domain.bar.entity.BarVisitSession;
import com.application.domain.bar.service.BarService;
import com.application.domain.bar.service.VisitSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * TrustLevelResolver 의 **진짜 구현** (T-11). 임시 InMemory 구현을 대체한다.
 *
 * 계약(인터페이스 javadoc):
 *   - sessionToken null/blank            → L0
 *   - 다른 바에 바인딩된 토큰            → L0
 *   - 만료/폐기 세션                     → L0
 *   - mock 의심 세션                     → 최대 L1 (세션 발급 시 이미 강등되어 저장됨)
 *   - **예외를 던지지 않는다.** 판정 불가 = L0 (fail-closed)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionTrustLevelResolver implements TrustLevelResolver {

    private final BarService barService;
    // VisitSessionService → BarService → (없음). 순환은 없지만 방어적으로 지연 주입.
    @Lazy
    private final VisitSessionService visitSessionService;

    @Override
    public TrustLevel resolve(String barSlug, String sessionToken) {
        if (barSlug == null || sessionToken == null || sessionToken.isBlank()) {
            return TrustLevel.L0;
        }
        try {
            Long barId = barService.getActiveBarOrThrow(barSlug).getId();
            BarVisitSession session = visitSessionService.findActiveSessionOrNull(barId, sessionToken);
            if (session == null) return TrustLevel.L0;

            // 방어: mock 의심 세션이 어떤 경로로든 L2 로 저장됐다면 여기서 다시 깎는다.
            if (Boolean.TRUE.equals(session.getMockSuspected())) {
                return TrustLevel.L1;
            }
            return session.getTrustLevel();
        } catch (Exception e) {
            log.debug("신뢰등급 판정 실패 → L0: slug={} {}", barSlug, e.toString());
            return TrustLevel.L0;   // fail-closed
        }
    }
}
