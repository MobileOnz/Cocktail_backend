package com.application.domain.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Admin 화면 라우팅 공통 컨트롤러.
 * Spring Security formLogin 이 /admin/login 을 loginPage 로 참조하지만
 * 해당 뷰를 서빙하는 컨트롤러가 없었기에, 템플릿을 반환하는 최소 라우터를 추가.
 *
 * SecurityConfig 는 건드리지 않고 기존 체인 위에서 동작.
 */
@Controller
@RequestMapping("/admin")
public class AdminViewController {

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {
        if (error != null) {
            model.addAttribute("errorMsg", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        if (logout != null) {
            model.addAttribute("logoutMsg", "로그아웃 되었습니다.");
        }
        return "admin/login";
    }

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/cocktails")
    public String cocktails() {
        return "admin/cocktails";
    }

    @GetMapping("/cocktail/detail")
    public String cocktailDetail() {
        return "admin/cocktail/detail";
    }

    @GetMapping("/cocktail/tag")
    public String tag() {
        return "admin/tag";
    }
}
