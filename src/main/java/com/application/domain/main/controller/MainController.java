package com.application.domain.main.controller;

import com.application.common.auth.dto.oauth2Dto.CustomOAuth2User;
import com.application.common.response.ResponseDto;
import com.application.domain.main.dto.MainResponse;
import com.application.domain.main.service.MainFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 메인 첫 화면 (T-09). OPTIONAL 인증 — 토큰이 있으면 개인화, 없으면 인기 기반.
 * 봉투 {code,msg,data}, data 내부 camelCase.
 */
@RestController
@RequestMapping("/api/v2/main")
@RequiredArgsConstructor
@Tag(name = "메인 API", description = "첫 화면 합성(추천 hero + 뉴스/가이드 피드)")
public class MainController {

    private final MainFeedService mainFeedService;

    @Operation(summary = "메인 첫 화면", description = "hero(오늘의 추천) + feed(뉴스/가이드 인터리브). 토큰 있으면 개인화.")
    @GetMapping
    public ResponseEntity<ResponseDto<MainResponse>> main(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomOAuth2User user,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false, defaultValue = "20") Integer size) {

        String credentialId = (user == null) ? null : user.getCredentialId();
        MainResponse data = mainFeedService.getMain(credentialId, cursor, size);
        return new ResponseEntity<>(ResponseDto.onSuccess("메인 조회 성공", data), HttpStatus.OK);
    }
}
