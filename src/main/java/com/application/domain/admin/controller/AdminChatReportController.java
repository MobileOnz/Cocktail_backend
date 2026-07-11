package com.application.domain.admin.controller;

import com.application.domain.admin.service.AdminBarService;
import com.application.domain.admin.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 채팅 신고 큐 (T-19). bar_chat_report → 미처리(VISIBLE) 신고 메시지 목록.
 * 처리 액션: 메시지 숨김(hide) / 작성자 차단(block = 그 바에서 해당 작성자 전체 숨김).
 * 24h SLA 표기(오래된 신고 우선). 모든 처리는 CSRF + ROLE_ADMIN + 감사 로그.
 */
@Controller
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminChatReportController {

    private final AdminBarService bars;
    private final AuditLogService audit;

    @GetMapping
    public String queue(Model model) {
        model.addAttribute("reports", bars.listOpenReports());
        return "admin/report/list";
    }

    @PostMapping("/{messageId}/hide")
    public String hide(@PathVariable long messageId, Model model) {
        Map<String, Object> before = bars.getMessage(messageId);
        bars.hideMessage(messageId);
        audit.log("HIDE", "bar_chat_message", messageId, before, Map.of("status", "HIDDEN"));
        return rowsFragment(model);
    }

    @PostMapping("/{messageId}/block")
    public String block(@PathVariable long messageId, Model model) {
        Map<String, Object> msg = bars.getMessage(messageId);
        long barId = ((Number) msg.get("bar_id")).longValue();
        String authorRef = String.valueOf(msg.get("author_ref"));
        int hidden = bars.blockAuthorInBar(barId, authorRef);
        audit.log("BLOCK_AUTHOR", "bar_chat_message", messageId, msg,
                Map.of("barId", barId, "authorRef", authorRef, "hiddenCount", hidden));
        return rowsFragment(model);
    }

    private String rowsFragment(Model model) {
        model.addAttribute("reports", bars.listOpenReports());
        return "admin/report/rows :: rows";
    }
}
