package com.application.domain.cocktail.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

/**
 * 칵테일-무드 매핑 테이블 (단순화된 구조)
 * Cocktail 엔티티와 String 타입의 무드 태그를 직접 연결하며, 무드 정보를 저장합니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "cocktail_mood")
public class CocktailMood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ⭐️ Cocktail 외래 키 (ManyToOne)
    // 컬럼명: cocktail_id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cocktail_id", nullable = false)
    private Cocktail cocktail;

    // ⭐️ 무드 태그 이름 (String으로 직접 저장)
    // 컬럼명: mood_name
    @Column(nullable = false, length = 50)
    @Comment("무드 태그 이름 (예: 클래식, 데이트, 식전)")
    private String moodName;

    // 생성자 (연관관계 편의 메서드에 사용)
    public CocktailMood(Cocktail cocktail, String moodName) {
        this.cocktail = cocktail;
        this.moodName = moodName;
    }
}