package com.application.domain.admin.service;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 관리자 대시보드용 집계 서비스.
 *
 * - 모든 데이터는 native SQL 로 조회한다. (date_trunc / generate_series 같은 PostgreSQL
 *   전용 함수가 필요한 case 가 많아 QueryDSL 보다 native 가 명료함)
 * - 읽기 전용 트랜잭션. DB 부하를 줄이기 위해 통계는 모두 단일 호출 단위로 캐스팅된 결과만 반환.
 * - 데이터 누수 가능 영역(예: search_history 의 비회원 행)은 그대로 카운트한다.
 *   "검색어 인기 순위" 는 회원/비회원 구분이 의미 없기 때문.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardService {

    private final EntityManager em;

    /* =================================================================
     *                           Public API
     * ================================================================= */

    @Transactional(readOnly = true)
    public OverviewMetrics getOverviewMetrics() {
        long totalMembers = ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM member").getSingleResult()).longValue();

        long todaySignups = ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM member WHERE created_at::date = CURRENT_DATE")
                .getSingleResult()).longValue();

        long last7dSignups = ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM member WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'")
                .getSingleResult()).longValue();

        // 활성 디바이스: monitoring 테이블에 등록된 device_number distinct 카운트.
        // monitoring.created_at/updated_at 모두 존재하지만 last_seen 의미는 약하므로
        // "지금까지 등록된 distinct device 수" 와 "최근 7일 활성"을 분리해서 반환.
        long activeDevicesAllTime = ((Number) em.createNativeQuery(
                "SELECT COUNT(DISTINCT device_number) FROM monitoring")
                .getSingleResult()).longValue();

        long activeDevicesLast7d = ((Number) em.createNativeQuery(
                "SELECT COUNT(DISTINCT device_number) FROM monitoring " +
                "WHERE COALESCE(updated_at, created_at) >= CURRENT_DATE - INTERVAL '7 days'")
                .getSingleResult()).longValue();

        long unansweredInquiries = ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM inquiry WHERE status = 'NEW'")
                .getSingleResult()).longValue();

        return OverviewMetrics.builder()
                .totalMembers(totalMembers)
                .todaySignups(todaySignups)
                .last7dSignups(last7dSignups)
                .activeDevicesAllTime(activeDevicesAllTime)
                .activeDevicesLast7d(activeDevicesLast7d)
                .unansweredInquiries(unansweredInquiries)
                .build();
    }

    /** 소셜 로그인 분포: { KAKAO: 8, NAVER: 2, GOOGLE: 2, APPLE: 2, UNKNOWN: 0 } */
    @Transactional(readOnly = true)
    public Map<String, Long> getSocialLoginBreakdown() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT COALESCE(NULLIF(social_login, ''), 'UNKNOWN') AS social, COUNT(*) " +
                "FROM member GROUP BY 1 ORDER BY 2 DESC").getResultList();

        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] r : rows) {
            String key = r[0] == null ? "UNKNOWN" : r[0].toString();
            long count = ((Number) r[1]).longValue();
            result.put(key, count);
        }
        return result;
    }

    /** 최근 N일간 일별 가입자 수. 가입이 0인 날도 0 으로 채워서 반환한다. */
    @Transactional(readOnly = true)
    public List<DailyCount> getDailySignups(int days) {
        int safeDays = Math.max(1, Math.min(days, 90));

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT day::date, COUNT(m.id) " +
                "FROM generate_series(CURRENT_DATE - (:offset || ' days')::interval, CURRENT_DATE, '1 day') AS day " +
                "LEFT JOIN member m ON m.created_at::date = day::date " +
                "GROUP BY day ORDER BY day")
                .setParameter("offset", safeDays - 1)
                .getResultList();

        return toDailyCounts(rows);
    }

    /** 최근 N일간 일별 활성 디바이스 수 (해당 날짜에 monitoring 행이 created/updated 된 distinct device). */
    @Transactional(readOnly = true)
    public List<DailyCount> getDailyActiveDevices(int days) {
        int safeDays = Math.max(1, Math.min(days, 90));

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT day::date, COUNT(DISTINCT mn.device_number) " +
                "FROM generate_series(CURRENT_DATE - (:offset || ' days')::interval, CURRENT_DATE, '1 day') AS day " +
                "LEFT JOIN monitoring mn " +
                "  ON COALESCE(mn.updated_at, mn.created_at)::date = day::date " +
                "GROUP BY day ORDER BY day")
                .setParameter("offset", safeDays - 1)
                .getResultList();

        return toDailyCounts(rows);
    }

    /**
     * 인기 칵테일: recommend_count + bookmark 수의 합산 점수로 정렬.
     * 현재 cocktail_bookmark 가 비어 있어도 recommend_count 만으로 동작.
     */
    @Transactional(readOnly = true)
    public List<TopCocktail> getTopCocktails(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT c.id, c.kor_name, c.recommend_count, c.hard_count, " +
                "       COALESCE(b.bookmark_count, 0) AS bookmark_count, " +
                "       (c.recommend_count + COALESCE(b.bookmark_count, 0)) AS score " +
                "FROM cocktail c " +
                "LEFT JOIN ( " +
                "    SELECT cocktail_id, COUNT(*) AS bookmark_count " +
                "    FROM cocktail_bookmark GROUP BY cocktail_id" +
                ") b ON b.cocktail_id = c.id " +
                "ORDER BY score DESC, c.recommend_count DESC, c.id ASC " +
                "LIMIT :lim")
                .setParameter("lim", safeLimit)
                .getResultList();

        List<TopCocktail> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(TopCocktail.builder()
                    .id(((Number) r[0]).longValue())
                    .korName((String) r[1])
                    .recommendCount(((Number) r[2]).longValue())
                    .hardCount(((Number) r[3]).longValue())
                    .bookmarkCount(((Number) r[4]).longValue())
                    .score(((Number) r[5]).longValue())
                    .build());
        }
        return out;
    }

    /** 인기 검색어 Top N. 회원/비회원 모두 포함. 빈 문자열/공백은 제외. */
    @Transactional(readOnly = true)
    public List<TopSearchTerm> getTopSearchTerms(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT TRIM(query_text) AS q, COUNT(*) " +
                "FROM search_history " +
                "WHERE query_text IS NOT NULL AND TRIM(query_text) <> '' " +
                "GROUP BY 1 ORDER BY 2 DESC, 1 ASC " +
                "LIMIT :lim")
                .setParameter("lim", safeLimit)
                .getResultList();

        List<TopSearchTerm> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(new TopSearchTerm((String) r[0], ((Number) r[1]).longValue()));
        }
        return out;
    }

    /** 문의 상태 분포: { NEW: x, READ: y, REPLIED: z }. 없는 상태는 0 으로 채움. */
    @Transactional(readOnly = true)
    public Map<String, Long> getInquiryStatusBreakdown() {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT status, COUNT(*) FROM inquiry GROUP BY status").getResultList();

        Map<String, Long> result = new LinkedHashMap<>();
        result.put("NEW", 0L);
        result.put("READ", 0L);
        result.put("REPLIED", 0L);
        for (Object[] r : rows) {
            String key = r[0] == null ? "UNKNOWN" : r[0].toString();
            long count = ((Number) r[1]).longValue();
            result.put(key, count);
        }
        return result;
    }

    /** 최근 가입한 회원 N명 (닉네임/소셜/가입일). */
    @Transactional(readOnly = true)
    public List<RecentSignup> getRecentSignups(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT id, nickname, social_login, created_at " +
                "FROM member ORDER BY created_at DESC NULLS LAST, id DESC " +
                "LIMIT :lim")
                .setParameter("lim", safeLimit)
                .getResultList();

        List<RecentSignup> out = new ArrayList<>();
        for (Object[] r : rows) {
            LocalDateTime createdAt = null;
            if (r[3] instanceof Timestamp ts) {
                createdAt = ts.toLocalDateTime();
            } else if (r[3] instanceof LocalDateTime ldt) {
                createdAt = ldt;
            }
            out.add(RecentSignup.builder()
                    .id(((Number) r[0]).longValue())
                    .nickname((String) r[1])
                    .socialLogin((String) r[2])
                    .createdAt(createdAt)
                    .build());
        }
        return out;
    }

    /* =================================================================
     *                            Helpers
     * ================================================================= */

    private List<DailyCount> toDailyCounts(List<Object[]> rows) {
        List<DailyCount> out = new ArrayList<>(rows.size());
        for (Object[] r : rows) {
            LocalDate date;
            if (r[0] instanceof java.sql.Date d) {
                date = d.toLocalDate();
            } else if (r[0] instanceof LocalDate ld) {
                date = ld;
            } else if (r[0] instanceof Timestamp ts) {
                date = ts.toLocalDateTime().toLocalDate();
            } else {
                date = LocalDate.parse(r[0].toString());
            }
            out.add(new DailyCount(date, ((Number) r[1]).longValue()));
        }
        return out;
    }

    /* =================================================================
     *                              DTOs
     * ================================================================= */

    @Getter
    @Builder
    public static class OverviewMetrics {
        private final long totalMembers;
        private final long todaySignups;
        private final long last7dSignups;
        private final long activeDevicesAllTime;
        private final long activeDevicesLast7d;
        private final long unansweredInquiries;
    }

    @Getter
    @AllArgsConstructor
    public static class DailyCount {
        private final LocalDate date;
        private final long count;
    }

    @Getter
    @Builder
    public static class TopCocktail {
        private final long id;
        private final String korName;
        private final long recommendCount;
        private final long hardCount;
        private final long bookmarkCount;
        private final long score;
    }

    @Getter
    @AllArgsConstructor
    public static class TopSearchTerm {
        private final String queryText;
        private final long count;
    }

    @Getter
    @Builder
    public static class RecentSignup {
        private final long id;
        private final String nickname;
        private final String socialLogin;
        private final LocalDateTime createdAt;
    }
}
