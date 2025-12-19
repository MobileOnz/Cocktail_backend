package com.application.domain.cocktail.entity.guide;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

/**
 * 가이드 세부 단계 엔티티.
 * 'guide_part' 외래키를 통해 Guide.part와 연결됩니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "guide_detail", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"guide_part", "display_order"})
})
public class GuideDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guide_part", referencedColumnName = "part", nullable = false) // ⭐️ Guide의 part 컬럼을 참조
    @Setter
    private Guide guide;

    @Column(name = "display_order", nullable = false)
    @Comment("순서")
    private Integer displayOrder;

    @Column(length = 255)
    private String subtitle;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(length = 1000)
    private String imageUrl;
}