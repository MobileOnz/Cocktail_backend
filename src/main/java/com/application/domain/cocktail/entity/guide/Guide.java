package com.application.domain.cocktail.entity.guide;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;

/**
 * 칵테일 가이드의 큰 주제(Part) 정보를 저장하는 엔티티입니다.
 * (예: Part 1. 칵테일이란?)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "guide")
public class Guide {

    @Id
    @Comment("가이드 파트 번호 (PK)")
    private Integer part;

    @Column(nullable = false, length = 255)
    @Comment("가이드의 전체 제목")
    private String title;

    @Column(length = 1000)
    @Comment("가이드 대표 이미지 URL")
    private String imageUrl;

    @Column(length = 50)
    @Comment("가이드 카테고리(탭 라벨). NULL 이면 FE 가 기타로 묶는다")
    private String category;

    // [관계 설정] GuideDetail과의 1:N 관계 (순서 보장을 위해 List 사용)
    // Part가 삭제되면 세부 내용도 함께 삭제됩니다.
    @Builder.Default
    @OneToMany(mappedBy = "guide", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC") // 순서대로 정렬하여 가져오도록 지정
    private List<GuideDetail> details = new ArrayList<>();

    // --- 연관관계 편의 메서드 ---
    public void addDetail(GuideDetail detail) {
        this.details.add(detail);
        detail.setGuide(this);
    }
}