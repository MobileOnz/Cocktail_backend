package com.application.domain.cocktail.service;

import com.application.domain.cocktail.dto.response.RecommendationResult;
import com.application.domain.member.enums.AgeRange;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 개인화 추천 (T-09). 기존 CocktailService / 5문항 추천은 건드리지 않고 룰 기반으로 새로 구현.
 *
 * <p>룰 우선순위 (위에서부터 적용, 후보가 없으면 다음 룰로 폴백):</p>
 * <ol>
 *   <li><b>LIKED_BASE</b> — 회원이 저장/좋아요/만든 칵테일이 있으면, 그중 가장 많은 base 를
 *       구해 같은 base 의 다른 칵테일(추천수 상위)을 제안. member_preference 를 갱신한다.</li>
 *   <li><b>COHORT</b> — 명시적 취향이 없고 온보딩(monitoring)에 연령대가 있으면,
 *       연령대 휴리스틱(젊을수록 약한 도수)으로 대표 칵테일 제안. 근거에 연령 라벨을 명시.</li>
 *   <li><b>POPULAR</b> — 그 외/비로그인: 지금 가장 많이 저장(bookmark)된 칵테일.</li>
 * </ol>
 * heroReason 은 위 룰에서 실제 신호(base/연령/저장수)로 생성한다 — 고정 문구 아님.
 */
@Service
@Slf4j
public class RecommendationService {

    @PersistenceContext
    private EntityManager em;

    /** 비로그인(=member 없음)일 때. */
    @Transactional(readOnly = true)
    public RecommendationResult recommendForAnonymous() {
        return popularHero();
    }

    /** 로그인 회원용 개인화. */
    @Transactional
    public RecommendationResult recommendForMember(Long memberId) {
        // 1) 명시적 취향 신호 수집
        List<Long> liked = toLongs(em.createNativeQuery(
                        "SELECT cocktail_id FROM cocktail_reaction WHERE member_id = :m AND reaction_type = 'RECOMMEND' " +
                        "UNION SELECT cocktail_id FROM cocktail_bookmark WHERE member_id = :m " +
                        "UNION SELECT cocktail_id FROM cocktail_made WHERE member_id = :m")
                .setParameter("m", memberId).getResultList());

        if (!liked.isEmpty()) {
            String base = firstString(em.createNativeQuery(
                            "SELECT base FROM cocktail WHERE id IN (:ids) AND base IS NOT NULL " +
                            "GROUP BY base ORDER BY count(*) DESC, base LIMIT 1")
                    .setParameter("ids", liked).getResultList());

            if (base != null) {
                Object[] cand = firstRow(em.createNativeQuery(
                                "SELECT id, kor_name, image_url, abv_band FROM cocktail " +
                                "WHERE base = :base AND id NOT IN (:ids) " +
                                "ORDER BY recommend_count DESC, id LIMIT 1")
                        .setParameter("base", base).setParameter("ids", liked).getResultList());
                if (cand != null) {
                    upsertPreference(memberId, base, bandCode(str(cand[3])));
                    String reason = "회원님이 즐겨 찾는 " + base + " 베이스로 골랐어요";
                    return result(cand, reason, "LIKED_BASE");
                }
            }
        }

        // 2) 온보딩 코호트 (monitoring 연령대)
        Object[] cohort = firstRow(em.createNativeQuery(
                        "SELECT age_range FROM monitoring WHERE member_id = :m AND age_range IS NOT NULL " +
                        "ORDER BY updated_at DESC NULLS LAST LIMIT 1")
                .setParameter("m", memberId).getResultList());
        if (cohort != null && cohort[0] != null) {
            int ageOrd = ((Number) cohort[0]).intValue();
            String ageLabel = ageLabel(ageOrd);
            String targetBand = ageOrd <= 2 ? "약함" : "보통";   // 25-29 이하 → 약한 도수
            Object[] cand = firstRow(em.createNativeQuery(
                            "SELECT id, kor_name, image_url, abv_band FROM cocktail " +
                            "WHERE abv_band = :band ORDER BY recommend_count DESC, id LIMIT 1")
                    .setParameter("band", targetBand).getResultList());
            if (cand != null) {
                String reason = ageLabel + " 이용자가 즐겨 찾는 " + tasteAdj(targetBand) + " 칵테일이에요";
                return result(cand, reason, "COHORT");
            }
        }

        // 3) 폴백
        return popularHero();
    }

    // ── 룰 3: 가장 많이 저장된 칵테일 ──────────────────────────────────────
    private RecommendationResult popularHero() {
        Object[] row = firstRow(em.createNativeQuery(
                "SELECT c.id, c.kor_name, c.image_url, c.abv_band, count(b.id) AS saves " +
                "FROM cocktail c LEFT JOIN cocktail_bookmark b ON b.cocktail_id = c.id " +
                "GROUP BY c.id ORDER BY saves DESC, c.recommend_count DESC, c.id LIMIT 1").getResultList());
        if (row == null) return null;
        long saves = row.length > 4 && row[4] != null ? ((Number) row[4]).longValue() : 0;
        String reason = saves > 0 ? "지금 가장 많이 저장된 칵테일이에요" : "오늘의 추천 칵테일이에요";
        return result(row, reason, "POPULAR");
    }

    // ── member_preference upsert ──────────────────────────────────────────
    private void upsertPreference(Long memberId, String baseSpirit, String abvRange) {
        em.createNativeQuery(
                        "INSERT INTO member_preference(member_id, base_spirit, abv_range, updated_at) " +
                        "VALUES (:m, :b, :r, now()) " +
                        "ON CONFLICT (member_id) DO UPDATE SET base_spirit = :b, abv_range = :r, updated_at = now()")
                .setParameter("m", memberId).setParameter("b", baseSpirit).setParameter("r", abvRange)
                .executeUpdate();
    }

    // ── 헬퍼 ──────────────────────────────────────────────────────────────
    private RecommendationResult result(Object[] r, String reason, String tag) {
        return new RecommendationResult(
                ((Number) r[0]).longValue(), str(r[1]), str(r[2]), reason, tag);
    }

    private static String bandCode(String band) {
        if ("약함".equals(band)) return "LOW";
        if ("보통".equals(band)) return "MID";
        if ("강함".equals(band)) return "HIGH";
        return null;
    }

    private static String tasteAdj(String band) {
        if ("약함".equals(band)) return "부담 없는";
        if ("보통".equals(band)) return "균형 잡힌";
        if ("강함".equals(band)) return "진한";
        return "인기";
    }

    private static String ageLabel(int ordinal) {
        AgeRange[] v = AgeRange.values();
        if (ordinal >= 0 && ordinal < v.length) return v[ordinal].getDescription();
        return "회원";
    }

    @SuppressWarnings("unchecked")
    private static List<Long> toLongs(List<?> rows) {
        return rows.stream().map(o -> ((Number) o).longValue()).toList();
    }

    private static Object[] firstRow(List<?> rows) {
        if (rows.isEmpty()) return null;
        Object o = rows.get(0);
        return (o instanceof Object[]) ? (Object[]) o : new Object[]{o};
    }

    private static String firstString(List<?> rows) {
        return rows.isEmpty() ? null : str(rows.get(0));
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
