package com.application.domain.bar.entity;

import com.application.domain.bar.trust.TrustLevel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * "지금 이 매장 안에 있다"는 증명. token 은 평문 저장하지 않는다(sha256 만).
 */
@Entity
@Table(name = "bar_visit_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BarVisitSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bar_id", nullable = false)
    private Long barId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "trust_level", nullable = false, length = 2)
    private TrustLevel trustLevel;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "last_lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal lastLat;

    @Column(name = "last_lng", nullable = false, precision = 10, scale = 7)
    private BigDecimal lastLng;

    @Column(name = "last_proof_at", nullable = false)
    private LocalDateTime lastProofAt;

    @Column(name = "renew_count", nullable = false)
    private Integer renewCount;

    @Column(name = "mock_suspected", nullable = false)
    private Boolean mockSuspected;

    @Column(name = "revoked", nullable = false)
    private Boolean revoked;

    @Column(name = "revoke_reason", length = 40)
    private String revokeReason;

    private BarVisitSession(Long barId, Long memberId, String tokenHash, TrustLevel trustLevel,
                           LocalDateTime expiresAt, BigDecimal lat, BigDecimal lng, boolean mockSuspected) {
        LocalDateTime now = LocalDateTime.now();
        this.barId = barId;
        this.memberId = memberId;
        this.tokenHash = tokenHash;
        this.trustLevel = trustLevel;
        this.issuedAt = now;
        this.expiresAt = expiresAt;
        this.lastLat = lat;
        this.lastLng = lng;
        this.lastProofAt = now;
        this.renewCount = 0;
        this.mockSuspected = mockSuspected;
        this.revoked = false;
    }

    public static BarVisitSession issue(Long barId, Long memberId, String tokenHash, TrustLevel trust,
                                        LocalDateTime expiresAt, BigDecimal lat, BigDecimal lng, boolean mock) {
        return new BarVisitSession(barId, memberId, tokenHash, trust, expiresAt, lat, lng, mock);
    }

    /** 갱신: 좌표 재증명 + 만료 연장. 신뢰등급은 재판정 결과로 덮어쓴다(강등 가능). */
    public void renew(TrustLevel trust, LocalDateTime expiresAt, BigDecimal lat, BigDecimal lng, boolean mock) {
        this.trustLevel = trust;
        this.expiresAt = expiresAt;
        this.lastLat = lat;
        this.lastLng = lng;
        this.lastProofAt = LocalDateTime.now();
        this.renewCount = this.renewCount + 1;
        this.mockSuspected = mock;
    }

    public void revoke(String reason) {
        this.revoked = true;
        this.revokeReason = reason;
    }

    public boolean isActive(LocalDateTime now) {
        return !Boolean.TRUE.equals(revoked) && expiresAt.isAfter(now);
    }
}
