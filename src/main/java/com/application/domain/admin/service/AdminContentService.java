package com.application.domain.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

/**
 * 관리자 CRUD 데이터 접근 (T-18). cocktail_step·가이드 등 엔티티와의 소유권 충돌을 피하려고
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
