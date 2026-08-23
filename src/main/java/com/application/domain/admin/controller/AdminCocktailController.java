package com.application.domain.admin.controller;

import com.application.common.response.ResponseDto;
import com.application.domain.admin.service.AdminCocktailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 관리자 칵테일 조회 REST 엔드포인트.
 *
 * <p>어드민 칵테일 관리 화면(templates/admin/cocktails.html)의 AG Grid 가 호출하는
 * {@code GET /admin/search/cocktail} 이 백엔드에 없어 404 → "조회 실패" 로 그리드가 뜨지 않던 문제 해결.
 * 세션 인증(ROLE_ADMIN, SecurityConfig order-1 체인)이 이미 {@code /admin/**} 를 강제하므로 별도 가드 불필요.</p>
 *
 * <p>응답 계약(그리드가 읽는 형태): {@code {code:1, msg, data:[ {id, cocktail_kr, cocktail_en, abv_band,
 * taste_level, createdAt}, ... ]}} — cocktails.html:134 가 {@code res.data.data} 를 배열로 기대한다
 * (페이징은 AG Grid 클라이언트 사이드).</p>
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminCocktailController {

    private final AdminCocktailService adminCocktailService;

    @GetMapping("/search/cocktail")
    public ResponseDto<List<Map<String, Object>>> searchCocktail(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) String cocktailName) {
        List<Map<String, Object>> rows = adminCocktailService.search(cocktailName, page, size);
        return ResponseDto.onSuccess("칵테일 조회 성공", rows);
    }
}
