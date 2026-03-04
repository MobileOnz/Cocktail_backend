package com.application.domain.cocktail.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * <pre>
 *     Entity: SearchHistory (검색어 기록 저장소)
 * </pre>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "search_history")
public class SearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Comment("사용자가 입력한 검색어")
    private String queryText;

    @Comment("사용자 식별 ID (로그인 유저)")
    private Long userId;

    @CreationTimestamp
    @Column(updatable = false)
    @Comment("검색 수행 시간")
    private LocalDateTime createdAt;
}