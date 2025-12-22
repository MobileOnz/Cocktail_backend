package com.application.domain.cocktail.service;

import com.application.domain.cocktail.dto.response.SearchHistoryResponseDto;
import com.application.domain.cocktail.entity.SearchHistory;
import com.application.domain.cocktail.repository.SearchHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;

    /**
     * 새로운 검색어를 기록에 추가합니다.
     */
    public void addSearchHistory(Long userId, String queryText) {

        if (queryText == null || queryText.isBlank()) {
            return;
        }

        String trimmedQuery = queryText.trim();

        // 중복 검색어가 있다면 먼저 삭제
        searchHistoryRepository.deleteByUserIdAndQueryText(userId, trimmedQuery);

        searchHistoryRepository.save(SearchHistory.builder()
                .userId(userId)
                .queryText(queryText)
                .build());
    }

    /**
     * 최근 검색 기록 목록을 가져옵니다.
     */
    @Transactional(readOnly = true)
    public List<SearchHistoryResponseDto> getHistoryList(Long userId) {
        return searchHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(SearchHistoryResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 검색 기록 하나를 삭제합니다.
     */
    public void removeHistory(Long id, Long userId) {
        searchHistoryRepository.deleteByIdAndUserId(id, userId);
    }

    /**
     * 사용자의 모든 검색 기록을 삭제합니다.
     */
    public void clearAllHistory(Long userId) {
        searchHistoryRepository.deleteByUserId(userId);
    }
}