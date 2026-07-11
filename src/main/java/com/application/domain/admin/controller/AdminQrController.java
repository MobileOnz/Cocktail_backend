package com.application.domain.admin.controller;

import com.application.domain.admin.service.AdminBarService;
import com.application.domain.admin.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 관리자 QR 플래카드 발급/회전 (T-19).
 *
 * <p>QR 이미지 렌더링 라이브러리(zxing 등)가 build.gradle 에 아직 없어서(s1 요청 대기),
 * 지금은 서명된 페이로드 텍스트와 인쇄용 플래카드 페이지(브라우저 인쇄로 PDF 저장)를 제공한다.
 * 라이브러리가 추가되면 buildPayload() 결과를 그대로 인코딩만 하면 되므로 신뢰모델은 불변.</p>
 *
 * <p>회전(rotate) = 기존 활성 플래카드 revoke + key_version+1 + 새 secret. 인쇄된 옛 QR 은
 * key_version 불일치로 즉시 무효화된다.</p>
 */
@Controller
@RequestMapping("/admin/bars/{barId}/qr")
@RequiredArgsConstructor
public class AdminQrController {

    private final AdminBarService bars;
    private final AuditLogService audit;

    @GetMapping
    public String list(@PathVariable long barId, Model model) {
        populate(barId, model);
        return "admin/qr/list";
    }

    /** 발급 또는 회전(둘 다 동일 진입점 — 활성본이 있으면 회전). */
    @PostMapping("/rotate")
    public String rotate(@PathVariable long barId,
                         @RequestParam(required = false) String label,
                         Model model) {
        int newVersion = bars.issueOrRotatePlacard(barId, label);
        audit.log("ROTATE", "bar_qr_placard", barId, null,
                Map.of("barId", barId, "newKeyVersion", newVersion));
        populate(barId, model);
        return "admin/qr/list :: activeBlock";
    }

    /** 인쇄용 플래카드 (브라우저 인쇄 → PDF). */
    @GetMapping("/placard")
    public String placard(@PathVariable long barId, Model model) {
        populate(barId, model);
        return "admin/qr/placard";
    }

    private void populate(long barId, Model model) {
        Map<String, Object> bar = bars.getBar(barId);
        Map<String, Object> active = bars.getActivePlacard(barId);
        model.addAttribute("bar", bar);
        model.addAttribute("placards", bars.listPlacards(barId));
        model.addAttribute("active", active);
        if (active != null) {
            String payload = bars.buildPayload(
                    String.valueOf(bar.get("slug")),
                    ((Number) active.get("key_version")).intValue(),
                    String.valueOf(active.get("secret")));
            model.addAttribute("payload", payload);
        }
    }
}
