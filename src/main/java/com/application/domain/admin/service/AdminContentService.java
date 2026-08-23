package com.application.domain.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 관리자 CRUD 데이터 접근 (T-18). s1(뉴스)·s1(cocktail_step) 엔티티와의 소유권 충돌을 피하려고
 * 전부 네이티브 SQL(JdbcTemplate)로 느슨하게 결합한다. 테이블 부재 시 안전하게 빈 결과/무시.
 */
@Service
@RequiredArgsConstructor
public class AdminContentService {

    private final JdbcTemplate jdbc;

    private boolean tableExists(String name) {
        Integer c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?", Integer.class, name);
        return c != null && c > 0;
    }

    // ─────────────────────────── 뉴스 (news, s1 V4) ───────────────────────────

    public boolean newsAvailable() { return tableExists("news"); }

    public List<Map<String, Object>> listNews() {
        if (!newsAvailable()) return List.of();
        return jdbc.queryForList(
                "SELECT id, title, category, featured, view_count, published_at, created_at " +
                "FROM news ORDER BY published_at DESC NULLS LAST, id DESC");
    }

    public Map<String, Object> getNews(long id) {
        return jdbc.queryForMap("SELECT * FROM news WHERE id = ?", id);
    }

    public long createNews(String title, String summary, String content, String category,
                           String source, LocalDateTime publishedAt) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO news (title, summary, content, category, source, published_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?)", new String[]{"id"});
            ps.setString(1, title);
            ps.setString(2, summary);
            ps.setString(3, content);
            ps.setString(4, category);
            ps.setString(5, source);
            ps.setTimestamp(6, publishedAt == null ? null : Timestamp.valueOf(publishedAt));
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? -1 : key.longValue();
    }

    public void updateNews(long id, String title, String summary, String content, String category,
                           String source, LocalDateTime publishedAt) {
        jdbc.update("UPDATE news SET title=?, summary=?, content=?, category=?, source=?, published_at=? WHERE id=?",
                title, summary, content, category, source,
                publishedAt == null ? null : Timestamp.valueOf(publishedAt), id);
    }

    public void deleteNews(long id) {
        jdbc.update("DELETE FROM news WHERE id = ?", id);
    }

    // ─────────────────────────── 가이드 (guide / guide_detail, V1) ───────────────────────────

    public List<Map<String, Object>> listGuides() {
        return jdbc.queryForList(
                "SELECT g.part, g.title, g.image_url, g.category, " +
                "  (SELECT COUNT(*) FROM guide_detail d WHERE d.guide_part = g.part) AS detail_count " +
                "FROM guide g ORDER BY g.part");
    }

    public Map<String, Object> getGuide(int part) {
        return jdbc.queryForMap("SELECT * FROM guide WHERE part = ?", part);
    }

    public List<Map<String, Object>> listGuideDetails(int part) {
        return jdbc.queryForList(
                "SELECT id, subtitle, display_order FROM guide_detail WHERE guide_part = ? ORDER BY display_order", part);
    }

    public void createGuide(int part, String title, String imageUrl, String category) {
        jdbc.update("INSERT INTO guide (part, title, image_url, category) VALUES (?, ?, ?, ?)", part, title, imageUrl, category);
    }

    public void updateGuide(int part, String title, String imageUrl, String category) {
        jdbc.update("UPDATE guide SET title=?, image_url=?, category=? WHERE part=?", title, imageUrl, category, part);
    }

    @Transactional
    public void deleteGuide(int part) {
        jdbc.update("DELETE FROM guide_detail WHERE guide_part = ?", part);
        jdbc.update("DELETE FROM guide WHERE part = ?", part);
    }

    // ─────────── 순서 변경: UNIQUE(부모, order) 충돌 회피용 3단 스왑 ───────────

    /** guide_detail 두 행의 display_order 를 맞바꾼다. */
    @Transactional
    public boolean swapGuideDetailOrder(long idA, long idB) {
        Integer oa = orderOf("guide_detail", "id", idA, "display_order");
        Integer ob = orderOf("guide_detail", "id", idB, "display_order");
        if (oa == null || ob == null) return false;
        jdbc.update("UPDATE guide_detail SET display_order = -1 WHERE id = ?", idA);
        jdbc.update("UPDATE guide_detail SET display_order = ? WHERE id = ?", oa, idB);
        jdbc.update("UPDATE guide_detail SET display_order = ? WHERE id = ?", ob, idA);
        return true;
    }

    // ─────────────────────────── 칵테일 제조 단계 (cocktail_step, s1 V3) ───────────────────────────

    public boolean stepsAvailable() { return tableExists("cocktail_step"); }

    public List<Map<String, Object>> listSteps(long cocktailId) {
        if (!stepsAvailable()) return List.of();
        return jdbc.queryForList(
                "SELECT id, step_order, instruction, tip, duration_sec FROM cocktail_step " +
                "WHERE cocktail_id = ? ORDER BY step_order", cocktailId);
    }

    public long addStep(long cocktailId, String instruction, String tip, Integer durationSec) {
        Integer maxOrder = jdbc.queryForObject(
                "SELECT COALESCE(MAX(step_order), 0) FROM cocktail_step WHERE cocktail_id = ?", Integer.class, cocktailId);
        int next = (maxOrder == null ? 0 : maxOrder) + 1;
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO cocktail_step (cocktail_id, step_order, instruction, tip, duration_sec) " +
                    "VALUES (?, ?, ?, ?, ?)", new String[]{"id"});
            ps.setLong(1, cocktailId);
            ps.setInt(2, next);
            ps.setString(3, instruction);
            ps.setString(4, tip);
            if (durationSec == null) ps.setNull(5, java.sql.Types.INTEGER); else ps.setInt(5, durationSec);
            return ps;
        }, kh);
        Number key = kh.getKey();
        return key == null ? -1 : key.longValue();
    }

    @Transactional
    public void deleteStep(long stepId) {
        jdbc.update("DELETE FROM cocktail_step WHERE id = ?", stepId);
    }

    /** 같은 칵테일 내 인접 스텝과 step_order 를 맞바꿔 순서를 위/아래로 이동. */
    @Transactional
    public boolean moveStep(long stepId, String direction) {
        Map<String, Object> cur = jdbc.queryForMap(
                "SELECT cocktail_id, step_order FROM cocktail_step WHERE id = ?", stepId);
        long cocktailId = ((Number) cur.get("cocktail_id")).longValue();
        int order = ((Number) cur.get("step_order")).intValue();
        String cmp = "up".equals(direction) ? "< ?" : "> ?";
        String sort = "up".equals(direction) ? "DESC" : "ASC";
        List<Map<String, Object>> neigh = jdbc.queryForList(
                "SELECT id, step_order FROM cocktail_step WHERE cocktail_id = ? AND step_order " + cmp +
                " ORDER BY step_order " + sort + " LIMIT 1", cocktailId, order);
        if (neigh.isEmpty()) return false;
        long otherId = ((Number) neigh.get(0).get("id")).longValue();
        int otherOrder = ((Number) neigh.get(0).get("step_order")).intValue();
        jdbc.update("UPDATE cocktail_step SET step_order = -1 WHERE id = ?", stepId);
        jdbc.update("UPDATE cocktail_step SET step_order = ? WHERE id = ?", order, otherId);
        jdbc.update("UPDATE cocktail_step SET step_order = ? WHERE id = ?", otherOrder, stepId);
        return true;
    }

    private Integer orderOf(String table, String idCol, long id, String orderCol) {
        List<Integer> r = jdbc.query("SELECT " + orderCol + " FROM " + table + " WHERE " + idCol + " = ?",
                (rs, i) -> rs.getInt(1), id);
        return r.isEmpty() ? null : r.get(0);
    }
}
