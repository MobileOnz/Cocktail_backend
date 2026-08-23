package com.application.domain.admin.controller;

import com.application.domain.admin.service.AdminBarService;
import com.application.domain.admin.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 관리자 바/메뉴 CRUD (T-19).
 * htmx: 목록/메뉴 테이블을 fragment 로 부분 갱신.
 * 모든 쓰기는 CSRF(admin 체인) + ROLE_ADMIN + 감사 로그(admin_audit_log).
 */
@Controller
@RequestMapping("/admin/bars")
@RequiredArgsConstructor
public class AdminBarController {

    private final AdminBarService bars;
    private final AuditLogService audit;

    // ─────────── 바 목록 / 폼 ───────────

    @GetMapping
    public String list(Model model) {
        model.addAttribute("bars", bars.listBars());
        return "admin/bar/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("bar", Map.of());
        model.addAttribute("mode", "create");
        return "admin/bar/form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        model.addAttribute("bar", bars.getBar(id));
        model.addAttribute("mode", "edit");
        return "admin/bar/form";
    }

    @PostMapping
    public String create(@RequestParam String slug,
                         @RequestParam String nameKo,
                         @RequestParam(required = false) String nameEn,
                         @RequestParam(required = false) String address,
                         @RequestParam(defaultValue = "0") double lat,
                         @RequestParam(defaultValue = "0") double lng,
                         @RequestParam(required = false) String phone,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) String heroImage,
                         @RequestParam(defaultValue = "ACTIVE") String status,
                         Model model) {
        long id = bars.createBar(slug, nameKo, nameEn, address, lat, lng, phone, description, heroImage, status);
        audit.log("CREATE", "bar", id, null, Map.of("slug", slug, "nameKo", nameKo, "status", status));
        return rowsFragment(model);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable long id,
                         @RequestParam String nameKo,
                         @RequestParam(required = false) String nameEn,
                         @RequestParam(required = false) String address,
                         @RequestParam(defaultValue = "0") double lat,
                         @RequestParam(defaultValue = "0") double lng,
                         @RequestParam(required = false) String phone,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) String heroImage,
                         @RequestParam(defaultValue = "ACTIVE") String status,
                         Model model) {
        Map<String, Object> before = bars.getBar(id);
        bars.updateBar(id, nameKo, nameEn, address, lat, lng, phone, description, heroImage, status);
        audit.log("UPDATE", "bar", id, before, Map.of("nameKo", nameKo, "status", status));
        return rowsFragment(model);
    }

    private String rowsFragment(Model model) {
        model.addAttribute("bars", bars.listBars());
        return "admin/bar/rows :: rows";
    }

    // ─────────── 메뉴 (카테고리 / 아이템) ───────────

    @GetMapping("/{barId}/menu")
    public String menu(@PathVariable long barId, Model model) {
        model.addAttribute("bar", bars.getBar(barId));
        model.addAttribute("categories", bars.listCategories(barId));
        model.addAttribute("items", bars.listItems(barId));
        return "admin/bar/menu";
    }

    @PostMapping("/{barId}/menu/categories")
    public String addCategory(@PathVariable long barId,
                              @RequestParam String nameKo,
                              @RequestParam(required = false) String nameEn,
                              @RequestParam(defaultValue = "0") double priority,
                              Model model) {
        long id = bars.createCategory(barId, nameKo, nameEn, priority);
        audit.log("CREATE", "bar_menu_category", id, null, Map.of("barId", barId, "nameKo", nameKo));
        return menuItemsFragment(barId, model);
    }

    @PostMapping("/{barId}/menu/items")
    public String addItem(@PathVariable long barId,
                          @RequestParam(required = false) Long categoryId,
                          @RequestParam String name,
                          @RequestParam(required = false) String nameEn,
                          @RequestParam String price,
                          @RequestParam(required = false) String description,
                          @RequestParam(defaultValue = "true") boolean available,
                          Model model) {
        long id = bars.createItem(barId, categoryId, name, nameEn, price, description, available);
        audit.log("CREATE", "bar_menu_item", id, null, Map.of("barId", barId, "name", name, "price", price));
        return menuItemsFragment(barId, model);
    }

    @PostMapping("/{barId}/menu/items/{itemId}")
    public String updateItem(@PathVariable long barId,
                             @PathVariable long itemId,
                             @RequestParam String name,
                             @RequestParam(required = false) String nameEn,
                             @RequestParam String price,
                             @RequestParam(required = false) String description,
                             @RequestParam(defaultValue = "true") boolean available,
                             Model model) {
        bars.updateItem(itemId, name, nameEn, price, description, available);
        audit.log("UPDATE", "bar_menu_item", itemId, null, Map.of("name", name, "price", price));
        return menuItemsFragment(barId, model);
    }

    @DeleteMapping("/{barId}/menu/items/{itemId}")
    public String deleteItem(@PathVariable long barId, @PathVariable long itemId, Model model) {
        bars.deleteItem(itemId);
        audit.log("DELETE", "bar_menu_item", itemId, null, null);
        return menuItemsFragment(barId, model);
    }

    private String menuItemsFragment(long barId, Model model) {
        model.addAttribute("bar", bars.getBar(barId));
        model.addAttribute("categories", bars.listCategories(barId));
        model.addAttribute("items", bars.listItems(barId));
        return "admin/bar/menu :: itemsBlock";
    }
}
