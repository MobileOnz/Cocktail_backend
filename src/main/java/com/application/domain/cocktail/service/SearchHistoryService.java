package com.application.domain.cocktail.service;

import com.application.common.auth.dto.oauth2Dto.CustomOAuth2User;
import com.application.domain.cocktail.dto.response.SearchHistoryResponseDto;
import com.application.domain.cocktail.entity.SearchHistory;
import com.application.domain.cocktail.repository.SearchHistoryRepository;
import com.application.domain.member.entity.Member;
import com.application.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SearchHistoryService {

    private final SearchHistoryRepository searchHistoryRepository;
    private final MemberRepository memberRepository;

    /**
     * 새로운 검색어를 기록에 추가합니다.
     */
    public void addSearchHistory(CustomOAuth2User user, String queryText) {

        if (queryText == null || queryText.isBlank()) {
            return;
        }

        String trimmedQuery = queryText.trim();

        if (user == null) {
            return;
        }
        String credentialId = user.getCredentialId();
        if (credentialId == null) return;

        Member member = memberRepository.findByCredentialId(credentialId);
        if (member == null) return;


        // 중복 검색어가 있다면 먼저 삭제
        searchHistoryRepository.deleteByUserIdAndQueryText(member.getId(), trimmedQuery);

        searchHistoryRepository.save(SearchHistory.builder()
                .userId(member.getId())
                .queryText(queryText)
                .build());
    }

    /**
     * 최근 검색 기록 목록을 가져옵니다.
     */
    @Transactional(readOnly = true)
    public List<SearchHistoryResponseDto> getHistoryList(CustomOAuth2User user) {

        if (user == null) {
            return Collections.emptyList();
        }
        String credentialId = user.getCredentialId();
        if (credentialId == null) return Collections.emptyList();

        Member member = memberRepository.findByCredentialId(credentialId);
        if (member == null) return Collections.emptyList();

        return searchHistoryRepository.findByUserIdOrderByCreatedAtDesc(member.getId())
                .stream()
                .map(SearchHistoryResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 검색 기록 하나를 삭제합니다.
     */
    public void removeHistory(Long id, CustomOAuth2User user) {

        if (user == null) {
            return;
        }
        String credentialId = user.getCredentialId();
        if (credentialId == null) return;

        Member member = memberRepository.findByCredentialId(credentialId);
        if (member == null) return;

        searchHistoryRepository.deleteByIdAndUserId(id, member.getId());
    }

    /**
     * 사용자의 모든 검색 기록을 삭제합니다.
     */
    public void clearAllHistory(CustomOAuth2User user) {
        if (user == null) {
            return;
        }
        String credentialId = user.getCredentialId();
        if (credentialId == null) return;

        Member member = memberRepository.findByCredentialId(credentialId);
        if (member == null) return;

        searchHistoryRepository.deleteByUserId(member.getId());
    }
}