package com.application.domain.admin.controller;

import com.application.domain.cocktail.service.CocktailStepGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 제조 단계 초안 일괄 생성.
 *
 * <p>105종 중 12종만 단계를 갖고 있어 나머지는 "만드는 법"이 빈 화면이었다.
 * 재료·잔·스타일에서 규칙으로 초안을 만들어 채운다.</p>
 *
 * <p>사람이 쓴 단계(source=MANUAL)는 건드리지 않는다. 생성분은 AUTO 로 남아
 * 어드민에서 다듬으면 MANUAL 로 승격되고, 그 뒤로는 재생성 대상에서 빠진다.</p>
 */
@RestController
@RequestMapping("/admin/cocktails/steps")
@RequiredArgsConstructor
public class AdminStepGenerationController {

    private final CocktailStepGenerator generator;

    /**
     * @param overwriteAuto 이미 생성해 둔 AUTO 초안까지 다시 만들지. 규칙을 고친 뒤 재생성할 때 true.
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generate(
            @RequestParam(defaultValue = "false") boolean overwriteAuto) {

        Map<String, Integer> made = generator.generateMissing(overwriteAuto);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("generatedCocktails", made.size());
        body.put("totalSteps", made.values().stream().mapToInt(Integer::intValue).sum());
        body.put("detail", made);
        return ResponseEntity.ok(body);
    }
}
