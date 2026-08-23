package com.application.domain.magazine.controller;

import com.application.domain.magazine.entity.MagazineArticle;
import com.application.domain.magazine.repository.MagazineArticleRepository;
import com.application.domain.magazine.service.AdminMagazineService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * 어드민 매거진 관리: JSON 붙여넣기 주입(slug upsert) + 목록/삭제.
 * /admin/** 보안 체인(폼 로그인 + ROLE_ADMIN + CSRF) 뒤에서 동작.
 */
@Controller
@RequestMapping("/admin/magazine")
@RequiredArgsConstructor
public class AdminMagazineController {

    private final MagazineArticleRepository repository;
    private final AdminMagazineService adminMagazineService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("articles",
                repository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt")));
        return "admin/magazine/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("json", "");
        return "admin/magazine/new";
    }

    @PostMapping
    public String create(@RequestParam String json,
                         @RequestParam(required = false) String publish,
                         Model model) {
        try {
            MagazineArticle saved = adminMagazineService.upsertFromJson(json, publish != null);
            return "redirect:/admin/magazine?saved=" + saved.getId();
        } catch (IllegalArgumentException e) {
            model.addAttribute("json", json);
            model.addAttribute("error", e.getMessage());
            return "admin/magazine/new";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id) {
        repository.deleteById(id);
        return "redirect:/admin/magazine";
    }
}
