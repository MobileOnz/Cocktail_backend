package com.application.domain.cocktail.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * "만들어봤어요" 기록 (T-07). cocktail_made 테이블, 복합 PK(member_id, cocktail_id).
 */
@Entity
@Table(name = "cocktail_made")
@IdClass(CocktailMade.Pk.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CocktailMade {

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Id
    @Column(name = "cocktail_id")
    private Long cocktailId;

    @CreationTimestamp
    @Column(name = "made_at", updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime madeAt;

    public CocktailMade(Long memberId, Long cocktailId) {
        this.memberId = memberId;
        this.cocktailId = cocktailId;
    }

    /** 복합 키 클래스. */
    @NoArgsConstructor
    public static class Pk implements Serializable {
        private Long memberId;
        private Long cocktailId;

        public Pk(Long memberId, Long cocktailId) {
            this.memberId = memberId;
            this.cocktailId = cocktailId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(memberId, pk.memberId) && Objects.equals(cocktailId, pk.cocktailId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(memberId, cocktailId);
        }
    }
}
