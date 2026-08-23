package com.application.domain.bar.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Node(prisma) `model bar_visit` 이주. UNIQUE(member_id, bar_id) 로 토글을 보장한다. */
@Entity
@Table(name = "bar_visit",
       uniqueConstraints = @UniqueConstraint(name = "uk_bar_visit_member_bar",
                                             columnNames = {"member_id", "bar_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BarVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "bar_id", nullable = false)
    private Long barId;

    @Column(name = "checked_at", nullable = false)
    private LocalDateTime checkedAt;

    private BarVisit(Long memberId, Long barId) {
        this.memberId = memberId;
        this.barId = barId;
        this.checkedAt = LocalDateTime.now();
    }

    public static BarVisit of(Long memberId, Long barId) {
        return new BarVisit(memberId, barId);
    }
}
