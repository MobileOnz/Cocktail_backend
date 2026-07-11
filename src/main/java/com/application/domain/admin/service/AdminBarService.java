package com.application.domain.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 관리자 바/메뉴/QR/신고 데이터 접근 (T-19).
 *
 * <p>s4 소유의 bar 도메인 엔티티/리포지토리를 <b>수정하지 않기</b> 위해, 그리고 s4가 그 코드를
 * 동시 편집 중이라 컴파일 결합을 피하기 위해, 이미 검증된(ddl-auto:validate 통과) V5/V6 테이블에
 * 네이티브 SQL(JdbcTemplate)로만 접근한다. (T-18의 AdminContentService 와 동일 전략)</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminBarService {

    private final JdbcTemplate jdbc;
    private static final SecureRandom RANDOM = new SecureRandom();

    // ─────────────────────────── 바 CRUD ───────────────────────────

    public List<Map<String, Object>> listBars() {
        return jdbc.queryForList(
                "SELECT b.id, b.slug, b.name_ko, b.name_en, b.address, b.status, " +
                "  (SELECT COUNT(*) FROM bar_menu_item i WHERE i.bar_id = b.id) AS menu_count, " +
                "  (SELECT COUNT(*) FROM bar_visit v WHERE v.bar_id = b.id) AS visit_count " +
                "FROM bar b ORDER BY b.name_ko");
    }

    public Map<String, Object> getBar(long id) {
        return jdbc.queryForMap("SELECT * FROM bar WHERE id = ?", id);
    }

    public long createBar(String slug, String nameKo, String nameEn, String address,
                          double lat, double lng, String phone, String description,
                          String heroImage, String status) {
        return jdbc.queryForObject(
                "INSERT INTO bar (slug, name_ko, name_en, address, lat, lng, phone, description, hero_image, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class, slug, nameKo, blankToNull(nameEn), address, lat, lng,
                blankToNull(phone), blankToNull(description), blankToNull(heroImage),
                status == null || status.isBlank() ? "ACTIVE" : status);
    }

    public void updateBar(long id, String nameKo, String nameEn, String address,
                          double lat, double lng, String phone, String description,
                          String heroImage, String status) {
        jdbc.update(
                "UPDATE bar SET name_ko=?, name_en=?, address=?, lat=?, lng=?, phone=?, description=?, " +
                "hero_image=?, status=?, updated_at=now() WHERE id=?",
                nameKo, blankToNull(nameEn), address, lat, lng, blankToNull(phone),
                blankToNull(description), blankToNull(heroImage), status, id);
    }

    // ─────────────────────────── 메뉴 카테고리 / 아이템 ───────────────────────────

    public List<Map<String, Object>> listCategories(long barId) {
        return jdbc.queryForList(
                "SELECT id, name_ko, name_en, priority FROM bar_menu_category WHERE bar_id = ? ORDER BY priority, id", barId);
    }

    public List<Map<String, Object>> listItems(long barId) {
        return jdbc.queryForList(
                "SELECT i.id, i.name, i.name_en, i.price, i.price_amount, i.is_available, " +
                "  c.name_ko AS category FROM bar_menu_item i " +
                "LEFT JOIN bar_menu_category c ON c.id = i.category_id " +
                "WHERE i.bar_id = ? ORDER BY i.priority, i.id", barId);
    }

    public long createCategory(long barId, String nameKo, String nameEn, double priority) {
        return jdbc.queryForObject(
                "INSERT INTO bar_menu_category (bar_id, name_ko, name_en, priority) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, barId, nameKo, blankToNull(nameEn), priority);
    }

    public long createItem(long barId, Long categoryId, String name, String nameEn,
                           String price, String description, boolean available) {
        Integer priceAmount = parsePrice(price);
        return jdbc.queryForObject(
                "INSERT INTO bar_menu_item (bar_id, category_id, name, name_en, price, price_amount, description, is_available) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id",
                Long.class, barId, categoryId, name, blankToNull(nameEn), price, priceAmount,
                blankToNull(description), available);
    }

    public void updateItem(long itemId, String name, String nameEn, String price,
                           String description, boolean available) {
        jdbc.update(
                "UPDATE bar_menu_item SET name=?, name_en=?, price=?, price_amount=?, description=?, " +
                "is_available=?, updated_at=now() WHERE id=?",
                name, blankToNull(nameEn), price, parsePrice(price), blankToNull(description), available, itemId);
    }

    public void deleteItem(long itemId) {
        jdbc.update("DELETE FROM bar_menu_item WHERE id = ?", itemId);
    }

    /** "18,000원" / "시가" → 18000 / null. 정렬·priceBand 산출용. */
    private Integer parsePrice(String price) {
        if (price == null) return null;
        String digits = price.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : Integer.valueOf(digits);
    }

    // ─────────────────────────── QR 플래카드 발급 / 회전 ───────────────────────────

    public List<Map<String, Object>> listPlacards(long barId) {
        return jdbc.queryForList(
                "SELECT id, key_version, label, issued_at, revoked_at, scan_count " +
                "FROM bar_qr_placard WHERE bar_id = ? ORDER BY key_version DESC", barId);
    }

    public Map<String, Object> getActivePlacard(long barId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, key_version, secret, label FROM bar_qr_placard " +
                "WHERE bar_id = ? AND revoked_at IS NULL ORDER BY key_version DESC LIMIT 1", barId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * 새 플래카드 발급. 기존 활성 플래카드가 있으면 폐기(revoke)하고 key_version 을 +1 한다(회전).
     * @return 새 key_version
     */
    @Transactional
    public int issueOrRotatePlacard(long barId, String label) {
        Integer maxVersion = jdbc.queryForObject(
                "SELECT COALESCE(MAX(key_version), 0) FROM bar_qr_placard WHERE bar_id = ?", Integer.class, barId);
        int nextVersion = (maxVersion == null ? 0 : maxVersion) + 1;

        // 기존 활성 플래카드 전부 폐기 (인쇄물 무효화)
        jdbc.update("UPDATE bar_qr_placard SET revoked_at = now() WHERE bar_id = ? AND revoked_at IS NULL", barId);

        String secret = randomSecret();
        jdbc.update(
                "INSERT INTO bar_qr_placard (bar_id, key_version, secret, label) VALUES (?, ?, ?, ?)",
                barId, nextVersion, secret, blankToNull(label));
        return nextVersion;
    }

    /**
     * 표시용 QR 페이로드. s4 스캔 계약과 동일:
     * {slug}.{keyVersion}.{base64url(HMAC-SHA256(secret, "slug|keyVersion"))}
     */
    public String buildPayload(String slug, int keyVersion, String secret) {
        String sig = hmacBase64Url(secret, slug + "|" + keyVersion);
        return slug + "." + keyVersion + "." + sig;
    }

    private String randomSecret() {
        byte[] b = new byte[24];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private String hmacBase64Url(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] out = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 계산 실패", e);
        }
    }

    // ─────────────────────────── 채팅 신고 큐 ───────────────────────────

    /** 미처리(아직 VISIBLE) 신고 메시지 목록. 최초 신고 시각 오름차순(오래된 것 먼저 = SLA 위험). */
    public List<Map<String, Object>> listOpenReports() {
        return jdbc.queryForList(
                "SELECT m.id, b.slug, b.name_ko AS bar_name, m.author_ref, m.content, m.report_count, " +
                "  MIN(r.created_at) AS first_reported, " +
                "  EXTRACT(EPOCH FROM (now() - MIN(r.created_at)))/3600.0 AS hours_open, " +
                "  string_agg(DISTINCT r.reason, ', ') AS reasons " +
                "FROM bar_chat_message m " +
                "JOIN bar b ON b.id = m.bar_id " +
                "JOIN bar_chat_report r ON r.message_id = m.id " +
                "WHERE m.status = 'VISIBLE' " +
                "GROUP BY m.id, b.slug, b.name_ko, m.author_ref, m.content, m.report_count " +
                "ORDER BY first_reported ASC");
    }

    public Map<String, Object> getMessage(long messageId) {
        return jdbc.queryForMap("SELECT id, bar_id, author_ref, content, status FROM bar_chat_message WHERE id = ?", messageId);
    }

    /** 메시지 숨김 처리. */
    public void hideMessage(long messageId) {
        jdbc.update("UPDATE bar_chat_message SET status='HIDDEN', hidden_reason='ADMIN_HIDE' " +
                "WHERE id=? AND status='VISIBLE'", messageId);
    }

    /** 해당 바에서 그 작성자의 VISIBLE 메시지 전부 숨김(작성자 차단). @return 숨긴 건수 */
    public int blockAuthorInBar(long barId, String authorRef) {
        return jdbc.update("UPDATE bar_chat_message SET status='HIDDEN', hidden_reason='ADMIN_BLOCK_AUTHOR' " +
                "WHERE bar_id=? AND author_ref=? AND status='VISIBLE'", barId, authorRef);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
