package com.application.domain.bar.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Node(prisma) `model bar` 를 그대로 이주한 엔티티.
 * 컬럼명/타입은 V5__bar.sql 과 1:1. ddl-auto:validate 로 검증된다.
 */
@Entity
@Table(name = "bar")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slug", nullable = false, unique = true, length = 50)
    private String slug;

    @Column(name = "name_ko", nullable = false, length = 100)
    private String nameKo;

    @Column(name = "name_en", length = 100)
    private String nameEn;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(name = "lng", nullable = false, precision = 10, scale = 7)
    private BigDecimal lng;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "hero_image", length = 500)
    private String heroImage;

    /** 영업시간. jsonb → Map. 프론트 BarDetail.hours 는 any 로 받는다. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hours", columnDefinition = "jsonb")
    private Map<String, String> hours;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "featured_weight", nullable = false)
    private Double featuredWeight;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
