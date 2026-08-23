package com.application.domain.bar.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Node(prisma) `model bar_menu_category` 이주. parent 자기참조 유지. */
@Entity
@Table(name = "bar_menu_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BarMenuCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bar_id", nullable = false)
    private Long barId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "name_ko", nullable = false, length = 100)
    private String nameKo;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(name = "priority", nullable = false)
    private Double priority;

    @Column(name = "category_image", length = 500)
    private String categoryImage;

    @Column(name = "hide_side_image", nullable = false)
    private Boolean hideSideImage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
