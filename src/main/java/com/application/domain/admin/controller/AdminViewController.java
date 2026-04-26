package com.application.domain.admin.controller;

import com.application.domain.admin.service.AdminDashboardService;
import com.application.domain.admin.service.AdminDashboardService.DailyCount;
import com.application.domain.admin.service.AdminDashboardService.OverviewMetrics;
import com.application.domain.admin.service.AdminDashboardService.RecentSignup;
import com.application.domain.admin.service.AdminDashboardService.TopCocktail;
import com.application.domain.admin.service.AdminDashboardService.TopSearchTerm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin 화면 라우팅 공통 컨트롤러.
 * Spring Security formLogin 이 /admin/login 을 loginPage 로 참조하지만
 * 해당 뷰를 서빙하는 컨트롤러가 없었기에, 템플릿을 반환하는 최소 라우터를 추가.
 *
 * SecurityConfig 는 건드리지 않고 기존 체인 위에서 동작.
 *
 * 대시보드(/admin/dashboard)는 AdminDashboardService 를 통해 메트릭을 모델에 주입한다.
 * Chart.js 가 차트 데이터를 그리기 위해 일부 모델 attribute 는 미리 JS-friendly 한
 * "라벨 배열 / 값 배열" 형태로 가공해서 넘긴다.
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminViewController {

    private static final int DAILY_RANGE_DAYS = 30;
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MM-dd");

    private final AdminDashboardService adminDashboardService;

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
    public String dashboard(Model model) {
        OverviewMetrics overview = adminDashboardService.getOverviewMetrics();
        Map<String, Long> social = adminDashboardService.getSocialLoginBreakdown();
        List<DailyCount> dailySignups = adminDashboardService.getDailySignups(DAILY_RANGE_DAYS);
        List<DailyCount> dailyDevices = adminDashboardService.getDailyActiveDevices(DAILY_RANGE_DAYS);
        List<TopCocktail> topCocktails = adminDashboardService.getTopCocktails(10);
        List<TopSearchTerm> topSearchTerms = adminDashboardService.getTopSearchTerms(10);
        Map<String, Long> inquiryStatus = adminDashboardService.getInquiryStatusBreakdown();
        List<RecentSignup> recentSignups = adminDashboardService.getRecentSignups(10);

        model.addAttribute("overview", overview);
        model.addAttribute("social", social);
        model.addAttribute("topCocktails", topCocktails);
        model.addAttribute("topSearchTerms", topSearchTerms);
        model.addAttribute("inquiryStatus", inquiryStatus);
        model.addAttribute("recentSignups", recentSignups);

        // Chart.js 용 라벨/데이터 배열로 분리해서 주입
        model.addAttribute("dailyLabels", buildDayLabels(dailySignups));
        model.addAttribute("dailySignupCounts", buildCountList(dailySignups));
        model.addAttribute("dailyDeviceCounts", buildCountList(dailyDevices));

        model.addAttribute("socialLabels", new ArrayList<>(social.keySet()));
        model.addAttribute("socialValues", new ArrayList<>(social.values()));

        return "admin/dashboard";
    }

    /**
     * AJAX 갱신용 JSON 엔드포인트. 같은 데이터를 다시 호출.
     * 운영자가 새로고침 없이 부분 갱신할 때 사용.
     */
    @GetMapping("/dashboard/data")
    @ResponseBody
    public Map<String, Object> dashboardData() {
        Map<String, Object> body = new HashMap<>();
        body.put("overview", adminDashboardService.getOverviewMetrics());
        body.put("social", adminDashboardService.getSocialLoginBreakdown());
        body.put("dailySignups", adminDashboardService.getDailySignups(DAILY_RANGE_DAYS));
        body.put("dailyActiveDevices", adminDashboardService.getDailyActiveDevices(DAILY_RANGE_DAYS));
        body.put("topCocktails", adminDashboardService.getTopCocktails(10));
        body.put("topSearchTerms", adminDashboardService.getTopSearchTerms(10));
        body.put("inquiryStatus", adminDashboardService.getInquiryStatusBreakdown());
        body.put("recentSignups", adminDashboardService.getRecentSignups(10));
        return body;
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

    /* ============================== helpers ============================== */

    private List<String> buildDayLabels(List<DailyCount> series) {
        List<String> labels = new ArrayList<>(series.size());
        for (DailyCount d : series) {
            labels.add(d.getDate().format(DAY_LABEL));
        }
        return labels;
    }

    private List<Long> buildCountList(List<DailyCount> series) {
        List<Long> counts = new ArrayList<>(series.size());
        for (DailyCount d : series) {
            counts.add(d.getCount());
        }
        return counts;
    }
}
