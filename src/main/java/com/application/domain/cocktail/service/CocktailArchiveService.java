package com.application.domain.cocktail.service;

import com.application.common.exception.custom.CustomApiException;
import com.application.domain.cocktail.dto.response.CocktailResponseDto;
import com.application.domain.cocktail.entity.Cocktail;
import com.application.domain.cocktail.repository.CocktailRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 보관함 — 내가 남긴 기록으로 칵테일을 되찾는 경로.
 *
 * <p>그동안 보관함에는 '저장'만 있었다. 만들어봤어요(cocktail_made)와
 * 좋아요·어려워요(cocktail_reaction)는 <b>남기는 API 만 있고 목록으로 되읽는 API 가 없어서</b>
 * 사용자가 자기 기록을 볼 방법이 없었다. 여기서 그 목록을 돌려준다.</p>
 *
 * <p>cocktail_made 는 엔티티가 없는 조인 테이블(member_id, cocktail_id, made_at)이라
 * id 만 네이티브로 뽑고 칵테일 본문은 기존 리포지토리로 채운다.</p>
 */
@Service
@RequiredArgsConstructor
public class CocktailArchiveService {

    private static final List<String> REACTIONS = List.of("RECOMMEND", "HARD");

    @PersistenceContext
    private EntityManager em;

    private final CocktailRepository cocktailRepository;

    /** 내가 만들어본 칵테일. 최근에 만든 순. */
    @Transactional(readOnly = true)
    public List<CocktailResponseDto> myMade(Long memberId) {
        List<Long> ids = em.createNativeQuery(
                        "SELECT cocktail_id FROM cocktail_made WHERE member_id = :m ORDER BY made_at DESC")
                .setParameter("m", memberId)
                .getResultList();
        return inGivenOrder(toLongs(ids));
    }

    /**
     * 내가 남긴 반응.
     *
     * @param type RECOMMEND(좋아요) 또는 HARD(어려워요)
     */
    @Transactional(readOnly = true)
    public List<CocktailResponseDto> myReactions(Long memberId, String type) {
        String t = type == null ? "" : type.trim().toUpperCase();
        if (!REACTIONS.contains(t)) {
            throw new CustomApiException("반응 종류는 RECOMMEND 또는 HARD 여야 합니다.");
        }
        List<Long> ids = em.createNativeQuery(
                        "SELECT cocktail_id FROM cocktail_reaction " +
                                "WHERE member_id = :m AND reaction_type = :t ORDER BY id DESC")
                .setParameter("m", memberId)
                .setParameter("t", t)
                .getResultList();
        return inGivenOrder(toLongs(ids));
    }

    /**
     * id 목록 순서를 그대로 유지해 칵테일을 채운다.
     * findAllById 는 순서를 보장하지 않아 '최근 순'이 무너진다.
     */
    private List<CocktailResponseDto> inGivenOrder(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Cocktail> byId = cocktailRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Cocktail::getId, Function.identity()));
        return ids.stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)   // 칵테일이 지워졌을 수 있다
                .map(CocktailResponseDto::from)
                .toList();
    }

    private List<Long> toLongs(List<?> rows) {
        return rows.stream()
                .map(r -> ((Number) r).longValue())
                .toList();
    }
}
