package com.application.domain.cocktail.entity;

import com.application.domain.cocktail.enums.ReactionType;
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
    name = "cocktail_reaction",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_member_cocktail_reaction", // 제약조건 이름
            columnNames = {"member_id", "cocktail_id"} // 한 유저는 한 칵테일에 하나의 반응만 가능
        )
    }
)
public class CocktailReaction {

    /**
     * 사용자와 칵테일을 연결하고, 사용자의 반응 저장
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cocktail_id", nullable = false)
    private Cocktail cocktail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReactionType reactionType;

    @Builder
    public CocktailReaction(Member member, Cocktail cocktail, ReactionType reactionType) {
        this.member = member;
        this.cocktail = cocktail;
        this.reactionType = reactionType;
    }

    public void updateReactionType(ReactionType reactionType) {
        this.reactionType = reactionType;
    }

}
