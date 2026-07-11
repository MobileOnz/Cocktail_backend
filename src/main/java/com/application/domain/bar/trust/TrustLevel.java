package com.application.domain.bar.trust;

/**
 * 매장 신뢰등급 (plan_FINAL §6.1).
 *
 *   L0 ANON     : 아무것도 안 함        → 메뉴 이름·설명·priceBand 만
 *   L1 GEO      : 로그인 + GPS 반경 내  → + 채팅 읽기/쓰기
 *   L2 VERIFIED : L1 + 유효한 QR 서명   → + 정확한 가격 노출
 */
public enum TrustLevel {
    L0, L1, L2;

    /** this 가 other 이상인가. 가격 게이트는 atLeast(L2) 로 판정한다. */
    public boolean atLeast(TrustLevel other) {
        return this.ordinal() >= other.ordinal();
    }
}
