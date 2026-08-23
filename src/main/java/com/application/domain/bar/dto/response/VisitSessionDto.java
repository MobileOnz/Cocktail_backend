package com.application.domain.bar.dto.response;

import com.application.domain.bar.entity.BarVisitSession;

import java.time.LocalDateTime;

/**
 * @param sessionToken 이후 요청의 X-Onz-Bar-Session 헤더에 담는다. **응답에서만 평문으로 존재**하고
 *                     DB 에는 sha256 만 남는다.
 */
public record VisitSessionDto(
        String sessionToken,
        String trustLevel,
        boolean priceVisible,
        boolean chatEnabled,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt,
        Integer renewCount,
        Double distanceM,
        String nickname,
        String authorRef
) {
    public static VisitSessionDto of(BarVisitSession s, String rawToken, double distanceM,
                                     String nickname, String authorRef) {
        boolean l2 = s.getTrustLevel().atLeast(com.application.domain.bar.trust.TrustLevel.L2);
        return new VisitSessionDto(rawToken, s.getTrustLevel().name(), l2, true,
                s.getIssuedAt(), s.getExpiresAt(), s.getRenewCount(),
                Math.round(distanceM * 10.0) / 10.0, nickname, authorRef);
    }
}
