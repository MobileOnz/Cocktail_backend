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

    @Column(nullable = false)
    @Comment("한글 이름")
    private String korName;

    @Column(nullable = false)
    @Comment("영어 이름")
    private String engName;

    @Convert(converter = AbvLevelConverter.class)
    @Comment("도수 Level : enum 관리")
    private AbvLevel abvBand; // WEEK, NORMAL, STRONG

    @Comment("최대 도수")
    private Integer maxAlcohol;

    @Comment("최소 도수")
    private Integer minAlcohol;

    // 맛 레벨 사용x
    @Convert(converter= TasteLevelConverter.class)
    @Comment("taste Level : enum 관리")
    private TasteLevel tasteLevel; // BEGINNER, INTERMEDIATE, ADVANCED

    @Column(columnDefinition = "TEXT")
    @Comment("origin text")
    private String originText;

    @Column(length = 1000)
    @Comment("이미지 URL")
    private String imageUrl;

    // FIXME enum으로 변경
    private String season;

    // season은 굳이 테이블로 만들 필요 없어보임
    // v1에서만 사용
    @ElementCollection(targetClass = Season.class)
    @CollectionTable(
            name = "cocktail_season",
            joinColumns = @JoinColumn(name = "cocktail_id")
    )
    @Enumerated(EnumType.STRING) // Enum 이름으로 저장
    @Column
    private List<Season> seasons = new ArrayList<>();

    // 사용 x -> 검색 등에 사용되지 않는 것 같고, 재료는 보여주기만 하면 되는 것 같아서 문자열로 사용
    @OneToMany(mappedBy = "cocktail", cascade = CascadeType.ALL, orphanRemoval = true)
    @Comment("ingredients")
    private List<Ingredient> ingredients = new ArrayList<>();

    // 재료 text
    private String ingredientsText;

    // FIXME ENUM으로 하면 좋을듯
    private String style;

    // FIXME ENUM으로 하면 좋을듯
    private String glassType;

    private String glassImageUrl;

    // FIXME ENUM으로 하면 좋을듯
    private String base;

    @Comment("Tags: 분위기 태그 리스트 (Mood 이름 직접 저장)")
    @OneToMany(mappedBy = "cocktail", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CocktailMood> moods = new ArrayList<>();

    @Comment("Tags: 맛 태그 리스트 (Flavor 이름 직접 저장)")
    @OneToMany(mappedBy = "cocktail", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CocktailFlavor> flavors = new ArrayList<>();

    // 태그 사용 x (세분화)
    @Comment("Tags : 분위기, 맛 종류")
    @OneToMany(mappedBy = "cocktail", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CocktailTag> tags = new ArrayList<>();

    @Builder.Default
    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer recommendCount = 0;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "integer default 0")
    private Integer hardCount = 0;

    @CreationTimestamp
    @Column(updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    // ⭐️ [추가] 이 칵테일을 즐겨찾기한 내역들 (양방향 매핑)
    @OneToMany(mappedBy = "cocktail", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CocktailBookmark> bookmarks = new ArrayList<>();

    // v1에서 사용
    private String cocktailEN;
    private String cocktailKR;

    /**
     * 비즈니스 로직 (setter 대신)
     */

    public void update(String engName, String korName
            , AbvLevel abvBand, Integer maxAlcohol, Integer minAlcohol
            , TasteLevel tasteLevel, String originText, String imageUrl){

        this.engName  = engName;
        this.korName  = korName;
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

    // ⭐️ [편의 메서드] 특정 사용자가 이 칵테일을 북마크했는지 확인
    public boolean isBookmarkedBy(Long userId) {
        if (userId == null) return false;

        // 내 북마크 리스트를 순회하며 userId가 일치하는지 확인
        return this.bookmarks.stream()
                .anyMatch(bookmark -> bookmark.getMember().getId().equals(userId));
    }

}
