package com.application.domain.admin.controller;

import com.application.domain.admin.service.AdminContentService;
import com.application.domain.admin.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 가이드 CRUD (T-18). guide(part) + guide_detail 순서 편집.
 * 모든 쓰기는 CSRF + ROLE_ADMIN + 감사 로그.
 */
@Controller
@RequestMapping("/admin/guides")
@RequiredArgsConstructor
public class AdminGuideController {

    private final AdminContentService content;
    private final AuditLogService audit;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("guides", content.listGuides());
        return "admin/guide/list";
    }

    @GetMapping("/{part}/details")
    public String details(@PathVariable int part, Model model) {
        model.addAttribute("part", part);
        model.addAttribute("details", content.listGuideDetails(part));
        return "admin/guide/details :: details";
    }

    @PostMapping
    public String create(@RequestParam int part,
                         @RequestParam String title,
                         @RequestParam(required = false) String imageUrl,
                         @RequestParam(required = false) String category,
                         Model model) {
        content.createGuide(part, title, imageUrl, category);
        audit.log("CREATE", "guide", part, null, Map.of("part", part, "title", title));
        return rows(model);
    }

    @PostMapping("/{part}")
    public String update(@PathVariable int part,
                         @RequestParam String title,
                         @RequestParam(required = false) String imageUrl,
                         @RequestParam(required = false) String category,
                         Model model) {
        Map<String, Object> before = content.getGuide(part);
        content.updateGuide(part, title, imageUrl, category);
        audit.log("UPDATE", "guide", part, before, Map.of("title", title));
        return rows(model);
    }

    @DeleteMapping("/{part}")
    public String delete(@PathVariable int part, Model model) {
        Map<String, Object> before = content.getGuide(part);
        content.deleteGuide(part);
        audit.log("DELETE", "guide", part, before, null);
        return rows(model);
    }

    /** guide_detail 순서 변경(두 행 스왑). */
    @PostMapping("/details/swap")
    public String swapDetail(@RequestParam long idA, @RequestParam long idB,
                             @RequestParam int part, Model model) {
        boolean ok = content.swapGuideDetailOrder(idA, idB);
        if (ok) audit.log("REORDER", "guide_detail", idA + "<->" + idB, null,
                Map.of("swapped", idA + "," + idB));
        model.addAttribute("part", part);
        model.addAttribute("details", content.listGuideDetails(part));
        return "admin/guide/details :: details";
    }

    private String rows(Model model) {
        model.addAttribute("guides", content.listGuides());
        return "admin/guide/rows :: rows";
    }
}
