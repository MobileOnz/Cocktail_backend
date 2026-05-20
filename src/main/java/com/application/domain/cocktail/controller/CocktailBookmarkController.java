package com.application.domain.cocktail.controller;

import com.application.common.auth.dto.oauth2Dto.CustomOAuth2User;
import com.application.common.response.ResponseDto;
import com.application.domain.cocktail.dto.request.BookmarkBatchRequest;
import com.application.domain.cocktail.dto.response.BookmarkBatchResponse;
import com.application.domain.cocktail.dto.response.BookmarkListResponse;
import com.application.domain.cocktail.dto.response.BookmarkToggleResponse;
import com.application.domain.cocktail.dto.response.CocktailResponseDto;
import com.application.domain.cocktail.service.CocktailBookmarkService;
import com.application.domain.member.entity.Member;
import com.application.domain.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v2/cocktails")
@RequiredArgsConstructor
@Tag(name = "칵테일 즐겨찾기 API", description = "칵테일 즐겨찾기 추가/삭제 및 목록 조회")
public class CocktailBookmarkController {

    private final CocktailBookmarkService bookmarkService;
    private final MemberService memberService;

    /**
     * 즐겨찾기 배치 토글 (추가/삭제)
     * POST /api/v2/cocktails/bookmarks/batch
     */
    @Operation(
            summary = "칵테일 즐겨찾기 배치 토글",
            description = "여러 칵테일을 한 번에 즐겨찾기 토글 처리합니다. " +
                    "각 칵테일에 대해 이미 즐겨찾기되어 있으면 삭제, 없으면 추가합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/bookmarks/batch")
    public ResponseEntity<ResponseDto<BookmarkBatchResponse>> toggleBookmarkBatch(
            @RequestBody BookmarkBatchRequest request,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        // Request 내용 확인
        log.info("[배치 즐겨찾기] 받은 Request: {}", request);
        log.info("[배치 즐겨찾기] cocktailIds: {}", request.getCocktailIds());

        // credentialId로 Member PK 조회
        String credentialId = customOAuth2User.getCredentialId();
        log.info("[배치 즐겨찾기] credentialId: {}", credentialId);

        Member member = memberService.getMemberByCredentialId(credentialId);
        log.info("[배치 즐겨찾기] Member 조회 결과: {}", member);

        if (member == null) {
            throw new IllegalArgumentException("User not found with credentialId: " + credentialId);
        }

        Long memberId = member.getId();

        List<Long> processedCocktailIds = bookmarkService.toggleBookmarkBatch(memberId, request.getCocktailIds());

        BookmarkBatchResponse response = BookmarkBatchResponse.builder()
                .cocktailIds(processedCocktailIds)
                .build();

        return new ResponseEntity<>(
                ResponseDto.onSuccess("즐겨찾기 배치 토글 성공", response),
                HttpStatus.OK
        );
    }

    /**
     * 즐겨찾기 토글 (추가/삭제)
     * POST /api/v2/cocktails/{cocktailId}/bookmarks
     */
    @Operation(
            summary = "칵테일 즐겨찾기 토글",
            description = "칵테일을 즐겨찾기에 추가하거나 삭제합니다. " +
                    "이미 즐겨찾기되어 있으면 삭제, 즐겨찾기되어 있지 않으면 추가합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/{cocktailId}/bookmarks")
    public ResponseEntity<ResponseDto<BookmarkToggleResponse>> toggleBookmark(
            @PathVariable Long cocktailId,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        // credentialId로 Member PK 조회
        Long memberId = memberService.getMemberByCredentialId(customOAuth2User.getCredentialId()).getId();

        boolean isBookmarked = bookmarkService.toggleBookmark(memberId, cocktailId);

        BookmarkToggleResponse response = BookmarkToggleResponse.builder()
                .cocktailId(cocktailId)
                .isBookmarked(isBookmarked)
                .build();

        return new ResponseEntity<>(
                ResponseDto.onSuccess("즐겨찾기 토글 성공", response),
                HttpStatus.OK
        );
    }

    /**
     * 내가 즐겨찾기한 칵테일 목록 조회
     * GET /api/v2/cocktails/bookmarks
     */
    @Operation(
            summary = "내가 즐겨찾기한 칵테일 목록 조회",
            description = "로그인한 사용자가 즐겨찾기한 칵테일 목록을 최근 즐겨찾기 순서대로 조회합니다. " +
                    "응답에는 사용자 ID(memberId, credentialId)와 즐겨찾기한 칵테일 목록이 포함됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/bookmarks")
    public ResponseEntity<ResponseDto<BookmarkListResponse>> getMyBookmarks(
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        String credentialId = customOAuth2User.getCredentialId();
        Member member = memberService.getMemberByCredentialId(credentialId);
        Long memberId = member.getId();

        List<CocktailResponseDto> bookmarkedCocktails =
                bookmarkService.getMyBookmarkedCocktails(memberId);

        BookmarkListResponse response = BookmarkListResponse.builder()
                .memberId(memberId)
                .credentialId(credentialId)
                .totalCount(bookmarkedCocktails.size())
                .cocktails(bookmarkedCocktails)
                .build();

        return new ResponseEntity<>(
                ResponseDto.onSuccess("즐겨찾기 목록 조회 성공", response),
                HttpStatus.OK
        );
    }
}
