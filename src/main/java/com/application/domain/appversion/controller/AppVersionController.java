package com.application.domain.appversion.controller;

import com.application.common.response.ResponseDto;
import com.application.domain.appversion.dto.AppVersionResponse;
import com.application.domain.appversion.service.AppVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 앱 업데이트 안내·강제 업데이트용 버전 정보. 로그인 전에 부르므로 PUBLIC. */
@RestController
@RequestMapping("/api/v2/app")
@RequiredArgsConstructor
@Tag(name = "앱 버전 API", description = "업데이트 안내·강제 업데이트")
public class AppVersionController {

    private final AppVersionService appVersionService;

    @Operation(summary = "앱 버전 정보",
            description = "현재 버전 < minVersion 이면 강제 업데이트, < latestVersion 이면 업데이트 안내.")
    @GetMapping("/version")
    public ResponseEntity<ResponseDto<AppVersionResponse>> version(
            @Parameter(description = "ios 또는 android") @RequestParam String platform) {
        return new ResponseEntity<>(
                ResponseDto.onSuccess("앱 버전 조회 성공", appVersionService.get(platform)), HttpStatus.OK);
    }
}
