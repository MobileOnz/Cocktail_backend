package com.application.domain.cocktail.repository;

import com.application.domain.cocktail.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    // 유저별 최근 검색어를 최신순으로 조회
    List<SearchHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 전체 삭제
    void deleteByUserId(Long userId);

    // 중복 체크
    boolean existsByUserIdAndQueryText(Long userId, String queryText);

    // 특정 기록 삭제
    void deleteByIdAndUserId(Long id, Long userId);
}