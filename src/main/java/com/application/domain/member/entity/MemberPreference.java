package com.application.domain.member.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 회원 선호도 (T-09). member_preference 테이블과 매핑.
 * 온보딩/추천 결과로 갱신되는 개인화 캐시. member_id 를 PK 로 공유(1:1).
 */
@Entity
@Table(name = "member_preference")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberPreference {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "base_spirit", length = 30)
    private String baseSpirit;

    @Column(length = 12)
    private String sweetness;

    @Column(name = "abv_range", length = 12)
    private String abvRange;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
}
