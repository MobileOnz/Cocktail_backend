package com.application.domain.cocktail.entity;

import com.application.common.time.BaseTimeEntity;
import com.application.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "cocktail_bookmark",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_member_cocktail_bookmark",
            columnNames = {"member_id", "cocktail_id"}
        )
    }
)
public class CocktailBookmark extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cocktail_id", nullable = false)
    private Cocktail cocktail;

    @Builder
    public CocktailBookmark(Member member, Cocktail cocktail) {
        this.member = member;
        this.cocktail = cocktail;
    }
}
