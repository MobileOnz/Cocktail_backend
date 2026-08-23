package com.application.domain.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 콘텐츠 디렉터 대시보드 지표 (T-20). "다음에 무엇을 만들 것인가"에 답한다.
 *
 * ⭐ 무결과 검색 쿼리 top 50 = 다음에 쓸 콘텐츠 목록 그 자체.
 * search_history 에 아직 result_count 컬럼이 없으므로(T-06 예정), "매칭 칵테일이 없는 쿼리"를
 * NOT EXISTS 로 산출한다. 훗날 result_count 가 생기면 정확판으로 교체 가능.
 */
@Service
@RequiredArgsConstructor
public class ContentMetricsService {

    private final JdbcTemplate jdbcTemplate;

    public record NoResultQuery(String query, long count) {}
    public record NamedCount(String name, long count) {}

    /** 검색됐지만 매칭 칵테일이 없는 쿼리 top N. */
    public List<NoResultQuery> getNoResultSearchTop(int limit) {
        return jdbcTemplate.query(
                "SELECT TRIM(query_text) AS q, COUNT(*) AS cnt " +
                "FROM search_history sh " +
                "WHERE TRIM(query_text) <> '' " +
                "  AND NOT EXISTS ( " +
                "    SELECT 1 FROM cocktail c " +
                "    WHERE c.kor_name ILIKE '%' || TRIM(sh.query_text) || '%' " +
                "       OR c.eng_name ILIKE '%' || TRIM(sh.query_text) || '%' " +
                "  ) " +
                "GROUP BY TRIM(query_text) " +
                "ORDER BY cnt DESC, q ASC " +
                "LIMIT ?",
                (rs, i) -> new NoResultQuery(rs.getString("q"), rs.getLong("cnt")),
                limit);
    }

    /** 제조 단계가 없는 인기(추천수) 칵테일 top N — 스텝 입력 작업 큐. cocktail_step 미존재 시 빈 목록. */
    public List<NamedCount> getPopularCocktailsMissingSteps(int limit) {
        Integer stepTable = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'cocktail_step'",
                Integer.class);
        if (stepTable == null || stepTable == 0) {
            return List.of();
        }
        return jdbcTemplate.query(
                "SELECT c.kor_name AS name, c.recommend_count AS cnt " +
                "FROM cocktail c " +
                "WHERE NOT EXISTS (SELECT 1 FROM cocktail_step s WHERE s.cocktail_id = c.id) " +
                "ORDER BY c.recommend_count DESC, c.kor_name ASC " +
                "LIMIT ?",
                (rs, i) -> new NamedCount(rs.getString("name"), rs.getLong("cnt")),
                limit);
    }

    /** 개발자/운영 탭 — 지금 가진 데이터로 산출 가능한 것들 (Micrometer/actuator 는 F-17, s1 담당). */
    public Map<String, Object> getOpsSnapshot() {
        long searchVolume7d = firstLong(
                "SELECT COUNT(*) FROM search_history WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'");
        long noResultDistinct = firstLong(
                "SELECT COUNT(*) FROM (SELECT DISTINCT TRIM(query_text) q FROM search_history sh " +
                "WHERE TRIM(query_text) <> '' AND NOT EXISTS (SELECT 1 FROM cocktail c " +
                "WHERE c.kor_name ILIKE '%'||TRIM(sh.query_text)||'%' OR c.eng_name ILIKE '%'||TRIM(sh.query_text)||'%')) t");
        long openInquiries = firstLong("SELECT COUNT(*) FROM inquiry WHERE status <> 'REPLIED'");
        long auditWrites7d = firstLong(
                "SELECT COUNT(*) FROM admin_audit_log WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'");
        return Map.of(
                "searchVolume7d", searchVolume7d,
                "noResultDistinctQueries", noResultDistinct,
                "openInquiries", openInquiries,
                "adminWrites7d", auditWrites7d);
    }

    // ─────────────────────────── 바 / 채팅 지표 (T-20 마무리) ───────────────────────────

    private boolean tableExists(String name) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?", Integer.class, name);
        return c != null && c > 0;
    }

    /** 바 방문(체크인) top N. bar_visit 미존재 시 빈 목록. */
    public List<NamedCount> getBarVisitTop(int limit) {
        if (!tableExists("bar_visit") || !tableExists("bar")) {
            return List.of();
        }
        return jdbcTemplate.query(
                "SELECT b.name_ko AS name, COUNT(v.id) AS cnt " +
                "FROM bar b LEFT JOIN bar_visit v ON v.bar_id = b.id " +
                "GROUP BY b.id, b.name_ko " +
                "ORDER BY cnt DESC, b.name_ko ASC " +
                "LIMIT ?",
                (rs, i) -> new NamedCount(rs.getString("name"), rs.getLong("cnt")),
                limit);
    }

    /**
     * 채팅 활동 스냅샷 — 최근 7일 메시지 수, 활성 바 수, 미처리 신고 수, 숨김 메시지 수.
     * bar_chat_message 미존재 시 0으로 채운다.
     */
    public Map<String, Object> getChatActivity() {
        if (!tableExists("bar_chat_message")) {
            return Map.of("messages7d", 0L, "activeBars7d", 0L, "openReports", 0L, "hiddenMessages", 0L);
        }
        long messages7d = firstLong(
                "SELECT COUNT(*) FROM bar_chat_message WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'");
        long activeBars7d = firstLong(
                "SELECT COUNT(DISTINCT bar_id) FROM bar_chat_message WHERE created_at >= CURRENT_DATE - INTERVAL '7 days'");
        long hiddenMessages = firstLong("SELECT COUNT(*) FROM bar_chat_message WHERE status = 'HIDDEN'");
        long openReports = tableExists("bar_chat_report") ? firstLong(
                "SELECT COUNT(DISTINCT m.id) FROM bar_chat_message m " +
                "JOIN bar_chat_report r ON r.message_id = m.id WHERE m.status = 'VISIBLE'") : 0L;
        return Map.of(
                "messages7d", messages7d,
                "activeBars7d", activeBars7d,
                "openReports", openReports,
                "hiddenMessages", hiddenMessages);
    }

    private long firstLong(String sql) {
        Long v = jdbcTemplate.queryForObject(sql, Long.class);
        return v == null ? 0L : v;
    }
}
