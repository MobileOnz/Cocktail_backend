package com.application.domain.cocktail.entity;

import com.application.domain.cocktail.converter.*;
import com.application.domain.cocktail.enums.AbvLevel;
import com.application.domain.cocktail.enums.Season;
import com.application.domain.cocktail.enums.TasteLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "cocktail")
public class Cocktail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cocktail_kr_name", nullable = false)
    private String cocktailKR;

    @Column(name = "cocktail_en_name", nullable = false)
    private String cocktailEN;

    @Column(name = "max_alcohol")
    private Integer maxAlcohol;

    @Column(name = "min_alcohol")
    private Integer minAlcohol;

    @Column(name = "origin_text", columnDefinition = "TEXT")
    private String originText;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Convert(converter = AbvLevelConverter.class)
    @Comment("도수 Level : enum 관리")
    private AbvLevel abvBand; // WEEK, NORMAL, STRONG

    @Convert(converter= TasteLevelConverter.class)
    @Comment("taste Level : enum 관리")
    private TasteLevel tasteLevel; // BEGINNER, INTERMEDIATE, ADVANCED

    @ElementCollection(targetClass = Season.class)
    @CollectionTable(
            name = "cocktail_season",
            joinColumns = @JoinColumn(name = "cocktail_id")
    )
    @Enumerated(EnumType.STRING) // Enum 이름으로 저장
    @Column(name = "season")
    private List<Season> seasons = new ArrayList<>();

    @OneToMany(mappedBy = "cocktail", cascade = CascadeType.ALL, orphanRemoval = true)
    @Comment("ingredients")
    private List<Ingredient> ingredients = new ArrayList<>();

    @Comment("Tags : 분위기, 맛 종류")
    @OneToMany(mappedBy = "cocktail", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CocktailTag> tags = new ArrayList<>();

    @Builder.Default
    @Column(name = "recommend_count", nullable = false)
    private Integer recommendCount = 0;

    @Builder.Default
    @Column(name = "hard_count", nullable = false)
    private Integer hardCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "update_at")
    private LocalDateTime updatedAt;

    /**
     * 비즈니스 로직 (setter 대신)
     */

    public void update(String cocktailEN, String cocktailKR
            , AbvLevel abvBand, Integer maxAlcohol, Integer minAlcohol
            , TasteLevel tasteLevel, String originText, String imageUrl){

        this.cocktailEN  = cocktailEN;
        this.cocktailKR  = cocktailKR;
        this.abvBand     = abvBand;
        this.maxAlcohol  = maxAlcohol;
        this.minAlcohol  = minAlcohol;
        this.tasteLevel  = tasteLevel;
        this.originText  = originText;
        this.imageUrl    = imageUrl;
    }

    // 연관관계 편의 메서드
    public void addIngredient(Ingredient ingredient) {
        ingredients.add(ingredient);
        ingredient.setCocktail(this);
    }

    public void addTag(Tag tag) {
        CocktailTag cocktailTag = new CocktailTag(this, tag);
        tags.add(cocktailTag);
    }

//    public Cocktail(){}
//
//    @Builder
//    public Cocktail(String cocktailEN, String cocktailKR
//                    , AbvLevel abvBand, Integer maxAlcohol, Integer minAlcohol
//                    , TasteLevel tasteLevel, String originText, String imageUrl
//                    , List<Season> seasons, List<Ingredient> ingredients
//                    , List<CocktailTag> tags
//                    )
//    {
//        this.cocktailEN  = cocktailEN;
//        this.cocktailKR  = cocktailKR;
//        this.abvBand     = abvBand;
//        this.maxAlcohol  = maxAlcohol;
//        this.minAlcohol  = minAlcohol;
//        this.tasteLevel  = tasteLevel;
//        this.originText  = originText;
//        this.imageUrl    = imageUrl;
//        this.seasons     = seasons;
//        this.ingredients = ingredients;
//        this.tags       = tags;
//    }
//

}
