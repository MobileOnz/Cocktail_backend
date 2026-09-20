package com.application.domain.admin.controller;

import com.application.domain.admin.service.AuditLogService;
import com.application.domain.inquiry.entity.Inquiry;
import com.application.domain.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

/**
 * 1:1 문의 관리.
 *
 * <p>대시보드가 진작 `/admin/inquiries` 로 링크를 걸고 미답변 건수까지 세고 있었는데
 * 정작 그 화면이 없어서 눌러보면 404 였다(QA 제보). 엔티티·테이블·리포지토리는 이미 있었고
 * 화면만 빠져 있었다.</p>
 *
 * <p>상태는 NEW → READ → REPLIED 로 흐른다. 목록을 열면 NEW 는 READ 로 바꾸지 않는다 —
 * 읽음 처리는 담당자가 명시적으로 누른다(목록을 스쳐 지나간 것과 실제로 본 것은 다르다).</p>
 */
@Controller
@RequestMapping("/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController {

    private final InquiryRepository inquiries;
    private final AuditLogService audit;

    @GetMapping
    public String list(Model model) {
        List<Inquiry> all = inquiries.findAllByOrderByCreatedAtDesc();
        model.addAttribute("inquiries", all);
        model.addAttribute("countNew", inquiries.countByStatus("NEW"));
        model.addAttribute("countRead", inquiries.countByStatus("READ"));
        model.addAttribute("countReplied", inquiries.countByStatus("REPLIED"));
        return "admin/inquiry/list";
    }

    /** 읽음 처리. 이미 답변한 건은 되돌리지 않는다. */
    @PostMapping("/{id}/read")
    @Transactional
    public String markRead(@PathVariable long id, Model model) {
        Inquiry inquiry = find(id);
        String before = inquiry.getStatus();
        if (!"REPLIED".equals(before)) {
            inquiry.updateStatus("READ");
            audit.log("READ", "inquiry", id,
                    Map.of("status", before), Map.of("status", "READ"));
        }
        return list(model);
    }

    /**
     * 답변 저장. 답변 본문은 엔티티가 상태·시각까지 같이 바꾼다(reply()).
     *
     * <p>여기서 메일을 보내지는 않는다 — 발송 경로가 아직 없다. 답변 내용은 저장되고
     * 상태만 REPLIED 로 바뀐다. 실제 회신은 담당자가 email 컬럼을 보고 직접 한다.</p>
     */
    @PostMapping("/{id}/reply")
    @Transactional
    public String reply(@PathVariable long id,
                        @RequestParam("reply") String replyText,
                        Model model) {
        Inquiry inquiry = find(id);
        String body = replyText == null ? "" : replyText.trim();
        if (body.isEmpty()) {
            // 빈 답변으로 REPLIED 를 만들면 "답변함"이 거짓이 된다.
            model.addAttribute("error", "답변 내용을 입력해 주세요.");
            return list(model);
        }
        String before = inquiry.getStatus();
        inquiry.reply(body);
        audit.log("REPLY", "inquiry", id,
                Map.of("status", before), Map.of("status", "REPLIED"));
        return list(model);
    }

    private Inquiry find(long id) {
        return inquiries.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "문의를 찾을 수 없습니다: " + id));
    }
}
