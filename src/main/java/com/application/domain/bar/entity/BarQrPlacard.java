package com.application.domain.bar.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 매장 벽에 인쇄되어 붙는 물리 QR. key_version 을 올려 폐기(회전)한다. */
@Entity
@Table(name = "bar_qr_placard")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BarQrPlacard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bar_id", nullable = false)
    private Long barId;

    @Column(name = "key_version", nullable = false)
    private Integer keyVersion;

    /** HMAC 키. 플래카드마다 독립. 절대 응답에 실리지 않는다. */
    @Column(name = "secret", nullable = false, length = 120)
    private String secret;

    @Column(name = "label", length = 60)
    private String label;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "scan_count", nullable = false)
    private Integer scanCount;

    public boolean isRevoked() {
        return revokedAt != null;
    }
}
