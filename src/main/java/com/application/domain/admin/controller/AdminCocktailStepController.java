package com.application.domain.admin.controller;

import com.application.domain.admin.service.AdminContentService;
import com.application.domain.admin.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 칵테일 제조 단계 편집 (T-18): 추가 / 삭제 / 순서 변경(위·아래).
 * cocktail_step 은 s1 V3 소유 테이블이라 엔티티 대신 네이티브 SQL(AdminContentService)로 접근.
 * 모든 쓰기는 CSRF + ROLE_ADMIN + 감사 로그.
 */
@Controller
@RequestMapping("/admin/cocktails/{cocktailId}/steps")
@RequiredArgsConstructor
public class AdminCocktailStepController {

    private final AdminContentService content;
    private final AuditLogService audit;

    @GetMapping
    public String list(@PathVariable long cocktailId, Model model) {
        model.addAttribute("cocktailId", cocktailId);
        model.addAttribute("stepsAvailable", content.stepsAvailable());
        model.addAttribute("steps", content.listSteps(cocktailId));
        return "admin/cocktail/steps";
    }

    @PostMapping
    public String add(@PathVariable long cocktailId,
                      @RequestParam String instruction,
                      @RequestParam(required = false) String tip,
                      @RequestParam(required = false) Integer durationSec,
                      Model model) {
        long id = content.addStep(cocktailId, instruction, tip, durationSec);
        audit.log("CREATE", "cocktail_step", id, null,
                Map.of("cocktailId", cocktailId, "instruction", instruction));
        return rows(cocktailId, model);
    }

    @DeleteMapping("/{stepId}")
    public String delete(@PathVariable long cocktailId, @PathVariable long stepId, Model model) {
        content.deleteStep(stepId);
        audit.log("DELETE", "cocktail_step", stepId, Map.of("cocktailId", cocktailId), null);
        return rows(cocktailId, model);
    }

    @PostMapping("/{stepId}/move")
    public String move(@PathVariable long cocktailId, @PathVariable long stepId,
                       @RequestParam String direction, Model model) {
        boolean ok = content.moveStep(stepId, direction);
        if (ok) audit.log("REORDER", "cocktail_step", stepId, null,
                Map.of("cocktailId", cocktailId, "direction", direction));
        return rows(cocktailId, model);
    }

    private String rows(long cocktailId, Model model) {
        model.addAttribute("cocktailId", cocktailId);
        model.addAttribute("steps", content.listSteps(cocktailId));
        return "admin/cocktail/steps :: rows";
    }
}
