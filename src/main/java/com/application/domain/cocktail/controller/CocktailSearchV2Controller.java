package com.application.domain.cocktail.controller;

import com.application.common.auth.dto.oauth2Dto.CustomOAuth2User;
import com.application.common.response.ResponseDto;
import com.application.domain.cocktail.dto.response.SearchHistoryResponseDto;
import com.application.domain.cocktail.service.SearchHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/cocktails/search")
@RequiredArgsConstructor
@Tag(name = "칵테일 검색 관련 API", description = "칵테일 최근 검색어 등")
public class CocktailSearchV2Controller {

    private final SearchHistoryService searchHistoryService;

    // TODO: 실제 프로젝트에서는 SecurityContextHolder나 @AuthenticationPrincipal로 유저 ID를 가져와야 함
    private final Long currentUserId = 0L;

    /**
     * [최근 검색어 조회]
     * GET /api/v2/cocktails/search/history
     */
    @Operation(summary = "칵테일 최근 검색어 조회", description = "현재는 사용자 식별하지 않음")
    @GetMapping("/history")
    public ResponseEntity<ResponseDto<List<SearchHistoryResponseDto>>> getRecentSearches(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomOAuth2User customOAuth2User
    ) {
        List<SearchHistoryResponseDto> data = searchHistoryService.getHistoryList(customOAuth2User);
        return new ResponseEntity<>(
                ResponseDto.onSuccess("최근 검색어 목록 조회 성공", data),
                HttpStatus.OK
        );
    }

    /**
     * [최근 검색어 저장]
     * POST /api/v2/cocktails/search/history
     * 사용자가 검색 버튼을 누르거나 검색을 완료했을 때 호출합니다.
     */
    @Operation(summary = "칵테일 최근 검색어 저장", description = "현재는 사용자 식별하지 않음")
    @PostMapping("/history")
    public ResponseEntity<ResponseDto<Void>> saveSearchQuery(
            @RequestParam String queryText,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomOAuth2User customOAuth2User
    ) {
        searchHistoryService.addSearchHistory(customOAuth2User, queryText);
        return new ResponseEntity<>(
                ResponseDto.onSuccess("검색 기록 저장 완료", null),
                HttpStatus.OK
        );
    }

    /**
     * [최근 검색어 개별 삭제]
     * DELETE /api/v2/cocktails/search/history/{id}
     */
    @Operation(summary = "칵테일 최근 검색어 삭제(개별)", description = "현재는 사용자 식별하지 않음")
    @DeleteMapping("/history/{id}")
    public ResponseEntity<ResponseDto<Void>> deleteSearchHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomOAuth2User customOAuth2User
    ) {
        searchHistoryService.removeHistory(id, customOAuth2User);
        return new ResponseEntity<>(
                ResponseDto.onSuccess("검색 기록 삭제 완료", null),
                HttpStatus.OK
        );
    }

    /**
     * [최근 검색어 전체 삭제]
     * DELETE /api/v2/cocktails/search/history/all
     */
    @Operation(summary = "칵테일 최근 검색어 삭제(전체)", description = "현재는 사용자 식별하지 않음")
    @DeleteMapping("/history/all")
    public ResponseEntity<ResponseDto<Void>> clearSearchHistory(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomOAuth2User customOAuth2User
    ) {
        searchHistoryService.clearAllHistory(customOAuth2User);
        return new ResponseEntity<>(
                ResponseDto.onSuccess("모든 검색 기록 초기화 완료", null),
                HttpStatus.OK
        );
    }

}