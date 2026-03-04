package com.application.domain.cocktail.service;

import com.application.domain.cocktail.dto.response.CocktailResponseDto;
import com.application.domain.cocktail.entity.Cocktail;
import com.application.domain.cocktail.entity.CocktailBookmark;
import com.application.domain.cocktail.repository.CocktailBookmarkRepository;
import com.application.domain.cocktail.repository.CocktailRepository;
import com.application.domain.member.entity.Member;
import com.application.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CocktailBookmarkService {

    private final CocktailBookmarkRepository bookmarkRepository;
    private final CocktailRepository cocktailRepository;
    private final MemberRepository memberRepository;
    private final jakarta.persistence.EntityManager entityManager;

    /**
     * 북마크 토글 (추가/삭제)
     * - 북마크가 없으면 추가
     * - 북마크가 있으면 삭제
     */
    @Transactional
    public boolean toggleBookmark(Long memberId, Long cocktailId) {
        log.info("[즐겨찾기 토글 시작] memberId={}, cocktailId={}", memberId, cocktailId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Cocktail cocktail = cocktailRepository.findById(cocktailId)
                .orElseThrow(() -> new IllegalArgumentException("Cocktail not found"));

        Optional<CocktailBookmark> existingBookmark =
                bookmarkRepository.findByMemberIdAndCocktailId(memberId, cocktailId);

        if (existingBookmark.isEmpty()) {
            // 북마크 추가
            log.info("[즐겨찾기 추가] memberId={}, cocktailId={}", memberId, cocktailId);
            CocktailBookmark saved = createBookmark(member, cocktail);
            log.info("[즐겨찾기 추가 완료] bookmarkId={}", saved.getId());
            return true; // 북마크됨
        } else {
            // 북마크 삭제
            log.info("[즐겨찾기 삭제] bookmarkId={}", existingBookmark.get().getId());
            removeBookmark(existingBookmark.get());
            log.info("[즐겨찾기 삭제 완료] memberId={}, cocktailId={}", memberId, cocktailId);
            return false; // 북마크 취소됨
        }
    }

    /**
     * 내가 북마크한 칵테일 목록 조회
     * 최근 북마크 순서대로 정렬
     */
    @Transactional(readOnly = true)
    public List<CocktailResponseDto> getMyBookmarkedCocktails(Long memberId) {
        log.info("[보관함 조회 시작] memberId={}", memberId);

        List<CocktailBookmark> bookmarks =
                bookmarkRepository.findByMemberIdOrderByCreatedAtDesc(memberId);

        log.info("[보관함 조회 완료] memberId={}, 결과 개수={}", memberId, bookmarks.size());

        return bookmarks.stream()
                .map(bookmark -> CocktailResponseDto.from(bookmark.getCocktail()))
                .collect(Collectors.toList());
    }

    /**
     * 북마크 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean isBookmarked(Long memberId, Long cocktailId) {
        return bookmarkRepository.existsByMemberIdAndCocktailId(memberId, cocktailId);
    }

    // ===== Private Helper Methods =====

    private CocktailBookmark createBookmark(Member member, Cocktail cocktail) {
        CocktailBookmark bookmark = CocktailBookmark.builder()
                .member(member)
                .cocktail(cocktail)
                .build();
        CocktailBookmark saved = bookmarkRepository.save(bookmark);
        entityManager.flush(); // DB에 즉시 반영
        return saved;
    }

    private void removeBookmark(CocktailBookmark bookmark) {
        bookmarkRepository.delete(bookmark);
        entityManager.flush(); // DB에 즉시 반영
    }
}
