package com.application.domain.bar.trust;

/**
 * 방문 세션 토큰 → 신뢰등급 판정.
 *
 * ⚠️ T-11(QR + 방문 세션)이 이 인터페이스의 진짜 구현체를 끼운다.
 *    현재는 {@link InMemoryTrustLevelResolver} 가 임시로 등록되어 있다.
 *
 * T-11 구현체가 지켜야 할 계약:
 *   - sessionToken 이 null/blank 이면 반드시 {@link TrustLevel#L0} 을 반환한다.
 *   - 토큰이 유효하더라도 그 토큰이 barSlug 가 가리키는 바에 바인딩된 것이 아니면 L0.
 *   - 만료(expiresAt) / 폐기(revokedAt) 된 세션은 L0.
 *   - isMocked 였던 세션은 최대 L1 (가격 노출 불가).
 *   - 예외를 던지지 않는다. 판정 불가 = L0.
 */
public interface TrustLevelResolver {

    /**
     * @param barSlug      대상 바의 slug (not null)
     * @param sessionToken 요청 헤더 {@code X-Onz-Bar-Session} 의 값. 없으면 null
     * @return 판정된 신뢰등급. 절대 null 아님
     */
    TrustLevel resolve(String barSlug, String sessionToken);
}
