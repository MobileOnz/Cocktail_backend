package com.application.domain.bar.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Node(prisma) `model bar_menu_item` 이주.
 * price 는 Node 계약대로 String(자유입력, "시가" 가능). priceAmount/cocktailId 는 additive.
 * cocktail 은 domain/cocktail 소유이므로 연관관계를 걸지 않고 FK 값만 보관한다.
 */
@Entity
@Table(name = "bar_menu_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BarMenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bar_id", nullable = false)
    private Long barId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "name_en", length = 255)
    private String nameEn;

    @Column(name = "price", nullable = false, length = 255)
    private String price;

    @Column(name = "price_amount")
    private Integer priceAmount;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "menu_image", length = 500)
    private String menuImage;

    @Column(name = "priority", nullable = false)
    private Double priority;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable;

    @Column(name = "cocktail_id")
    private Long cocktailId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
