package com.application.domain.cocktail.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

/**
 * 칵테일-맛 매핑 테이블 (단순화된 구조)
 * Cocktail 엔티티와 String 타입의 맛 이름을 직접 연결하며, Flavor 태그를 저장합니다.
 * (이전의 별도 Flavor 마스터 테이블 없이, 칵테일과 태그 이름만 1:N으로 연결)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "cocktail_flavor")
public class CocktailFlavor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ⭐️ Cocktail 외래 키 (ManyToOne)
    // 컬럼명: cocktail_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cocktail_id", nullable = false)
    private Cocktail cocktail;

    // ⭐️ 맛 태그 이름 (String으로 직접 저장)
    // 컬럼명: flavor_name
    @Column(nullable = false, length = 50)
    @Comment("맛 태그 이름 (예: 쌉싸름, 허브향)")
    private String flavorName;

    // 생성자 (연관관계 편의 메서드에 사용)
    public CocktailFlavor(Cocktail cocktail, String flavorName) {
        this.cocktail = cocktail;
        this.flavorName = flavorName;
    }
}