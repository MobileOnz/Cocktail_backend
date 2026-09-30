package com.application.domain.admin.controller;

import com.application.domain.magazine.service.AdminStoryService;
import com.application.domain.admin.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 뉴스 CRUD (T-18). 마크다운 본문 + 예약 발행(published_at 미래값).
 * 저장은 magazine_article(STORY) 로 간다 — 앱 상세가 매거진에서 읽기 때문(#147). news 테이블은 더 쓰지 않는다.
 * htmx: 목록 테이블을 fragment(admin/news/rows :: rows)로 부분 갱신.
 * 모든 쓰기는 CSRF(admin 체인) + ROLE_ADMIN + 감사 로그.
 */
@Controller
@RequestMapping("/admin/news")
@RequiredArgsConstructor
public class AdminNewsController {

    private final AdminStoryService content;
    private final AuditLogService audit;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("newsAvailable", true);
        model.addAttribute("newsList", content.list());
        return "admin/news/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("news", Map.of());
        model.addAttribute("mode", "create");
        return "admin/news/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        model.addAttribute("news", content.get(id));
        model.addAttribute("mode", "edit");
        return "admin/news/form";
    }

    @PostMapping
    public String create(@RequestParam String title,
                         @RequestParam(required = false) String summary,
                         @RequestParam(required = false) String content_,
                         @RequestParam(defaultValue = "TREND") String category,
                         @RequestParam(required = false) String source,
                         @RequestParam(required = false) String publishedAt,
                         Model model) {
        LocalDateTime pub = parseDateTime(publishedAt);
        long id = content.create(title, summary, content_, category, source, pub);
        audit.log("CREATE", "magazine", id, null,
                Map.of("title", title, "category", category, "publishedAt", String.valueOf(pub)));
        return rowsFragment(model);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable long id,
                         @RequestParam String title,
                         @RequestParam(required = false) String summary,
                         @RequestParam(required = false) String content_,
                         @RequestParam(defaultValue = "TREND") String category,
                         @RequestParam(required = false) String source,
                         @RequestParam(required = false) String publishedAt,
                         Model model) {
        Map<String, Object> before = content.get(id);
        LocalDateTime pub = parseDateTime(publishedAt);
        content.update(id, title, summary, content_, category, source, pub);
        audit.log("UPDATE", "magazine", id, before,
                Map.of("title", title, "category", category, "publishedAt", String.valueOf(pub)));
        return rowsFragment(model);
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable long id, Model model) {
        Map<String, Object> before = content.get(id);
        content.delete(id);
        audit.log("DELETE", "magazine", id, before, null);
        return rowsFragment(model);
    }

    private String rowsFragment(Model model) {
        model.addAttribute("newsList", content.list());
        return "admin/news/rows :: rows";
    }

    private LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) return LocalDateTime.now();
        try {
            // HTML datetime-local: yyyy-MM-ddTHH:mm
            return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}
