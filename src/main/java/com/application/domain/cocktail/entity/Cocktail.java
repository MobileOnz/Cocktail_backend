package com.application.domain.cocktail.entity;

import com.application.domain.cocktail.converter.*;
import com.application.domain.cocktail.enums.AbvLevel;
import com.application.domain.cocktail.enums.Season;
import com.application.domain.cocktail.enums.TasteLevel;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import org.hibernate.annotations.Comment;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity(name = "cocktail")
public class Cocktail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    @Comment("Cocktail KR Name")
    private String cocktailKR;
    @Column
    @Comment("Cocktail EN Name")
    private String cocktailEN;

    @Column
    @Comment("max Alcohol")
    private Integer maxAlcohol;
    @Column
    @Comment("min Alcohol")
    private Integer minAlcohol;
    @Column(columnDefinition = "TEXT")
    @Comment("origin text")
    private String originText;
    @Column(length = 1000)
    @Comment("image URL")
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



    public Cocktail(){}

    @Builder
    public Cocktail(String cocktailEN, String cocktailKR
                    , AbvLevel abvBand, Integer maxAlcohol, Integer minAlcohol
                    , TasteLevel tasteLevel, String originText, String imageUrl
                    , List<Season> seasons, List<Ingredient> ingredients
                    , List<CocktailTag> tags
                    )
    {
        this.cocktailEN  = cocktailEN;
        this.cocktailKR  = cocktailKR;
        this.abvBand     = abvBand;
        this.maxAlcohol  = maxAlcohol;
        this.minAlcohol  = minAlcohol;
        this.tasteLevel  = tasteLevel;
        this.originText  = originText;
        this.imageUrl    = imageUrl;
        this.seasons     = seasons;
        this.ingredients = ingredients;
        this.tags       = tags;
    }

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

}
