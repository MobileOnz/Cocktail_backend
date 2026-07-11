package com.application.domain.cocktail.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 칵테일 제조 도구 (T-07). cocktail_tool 테이블과 매핑.
 * cocktail_tool_map(칵테일↔도구)은 엔티티 없이 네이티브 쿼리로만 조회한다.
 */
@Entity
@Table(name = "cocktail_tool")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CocktailTool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String name;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;
}
