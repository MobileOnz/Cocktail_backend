package com.application.domain.cocktail.controller;

import com.application.common.Constant;
import com.application.common.auth.dto.oauth2Dto.CustomOAuth2User;
import com.application.common.response.ResponseDto;
import com.application.domain.cocktail.dto.response.CocktailGuideDto;
import com.application.domain.cocktail.dto.response.CocktailStepDto;
import com.application.domain.cocktail.service.CocktailStepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * T-07 컨트롤러. 제조 단계 조회(PUBLIC) + "만들어봤어요"(JWT).
 * 기존 CocktailV2Controller 와 같은 base path 를 쓰지만 하위 경로(/steps, /made)가 달라 충돌 없음.
 */
@RestController
@RequestMapping("/api/v2/cocktails")
@RequiredArgsConstructor
@Tag(name = "칵테일 레시피 단계 API", description = "제조 단계 조회 및 만들기 기록")
public class CocktailStepController {

    private final CocktailStepService stepService;

    @Operation(summary = "제조 단계 조회", description = "칵테일의 제조 단계 목록(PUBLIC).")
    @GetMapping("/{cocktailId}/steps")
    public ResponseEntity<ResponseDto<List<CocktailStepDto>>> getSteps(@PathVariable Long cocktailId) {
        List<CocktailStepDto> steps = stepService.getSteps(cocktailId);
        return new ResponseEntity<>(ResponseDto.onSuccess("제조 단계 조회 성공", steps), HttpStatus.OK);
    }

    @Operation(summary = "이 칵테일의 이야기", description = "칵테일에 연결된 가이드 목록(PUBLIC).")
    @GetMapping("/{cocktailId}/guides")
    public ResponseEntity<ResponseDto<List<CocktailGuideDto>>> getGuides(@PathVariable Long cocktailId) {
        List<CocktailGuideDto> guides = stepService.getGuides(cocktailId);
        return new ResponseEntity<>(ResponseDto.onSuccess("연결된 가이드 조회 성공", guides), HttpStatus.OK);
    }

    @Operation(summary = "만들어봤어요 기록", description = "개인화 신호 저장(JWT 필요).")
    @PostMapping("/{cocktailId}/made")
    public ResponseEntity<ResponseDto<Void>> recordMade(
            @PathVariable Long cocktailId,
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomOAuth2User user) {

        if (user == null) {
            return new ResponseEntity<>(
                    ResponseDto.onFail(Constant.ERROR_CODE, "인증이 필요합니다."), HttpStatus.UNAUTHORIZED);
        }
        boolean ok = stepService.recordMade(user.getCredentialId(), cocktailId);
        if (!ok) {
            return new ResponseEntity<>(
                    ResponseDto.onFail(Constant.ERROR_CODE, "칵테일 또는 회원을 찾을 수 없습니다."), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(ResponseDto.onSuccess("기록되었습니다.", null), HttpStatus.OK);
    }
}
