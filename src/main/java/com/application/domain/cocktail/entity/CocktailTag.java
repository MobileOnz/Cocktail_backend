package com.application.domain.cocktail.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;



/***
 * Cocktail <    Cocktail_tag     >     tag
 *  ( N )   :          1         :      (N)
 */
@Getter
@Entity
@Table(name="cocktail_tag")
@NoArgsConstructor
public class CocktailTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cocktail_id", nullable = false)
    private Cocktail cocktail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    public CocktailTag(Cocktail cocktail, Tag tag) {
        this.cocktail = cocktail;
        this.tag = tag;
    }
}
