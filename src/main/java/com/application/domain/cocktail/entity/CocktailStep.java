package com.application.domain.cocktail.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 칵테일 제조 단계 (T-07). cocktail_step 테이블과 1:1 매핑.
 * cocktail 은 FK(Long)로만 참조 — Cocktail 엔티티는 다른 세션 소유이므로 연관 매핑하지 않는다.
 */
@Entity
@Table(name = "cocktail_step")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CocktailStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cocktail_id", nullable = false)
    private Long cocktailId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String instruction;

    @Column(columnDefinition = "TEXT")
    private String tip;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @CreationTimestamp
    @Column(updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
}
