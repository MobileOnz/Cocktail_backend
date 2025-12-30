package com.application.domain.monitoring.controller;

import com.application.domain.monitoring.dto.ReqTrackingDto;
import com.application.domain.monitoring.dto.ResMonitoringInfoDto;
import com.application.domain.monitoring.dto.ResTrackingDto;
import com.application.domain.monitoring.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/monitoring")
@RequiredArgsConstructor
@Tag(name = "모니터링", description = "페이지 접근 추적 API")
public class MonitoringController {

    private final MonitoringService monitoringService;

    @Operation(
            summary = "페이지 접근 추적",
            description = "기기 고유 번호와 접근 횟수를 전달하여 페이지 접근을 추적합니다. " +
                    "최초 접근 시 DB에 레코드를 생성하고, 이후 접근 시 count를 업데이트합니다. " +
                    "생성 시간(createdAt)은 최초 접근 시점으로 자동 저장됩니다."
    )
    @PostMapping("/track")
    public ResponseEntity<ResTrackingDto> trackPageAccess(@Valid @RequestBody ReqTrackingDto reqTrackingDto) {
        ResTrackingDto response = monitoringService.trackPageAccess(reqTrackingDto);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "기기 정보 조회",
            description = "기기 고유 번호로 기기 정보, 연령, 성별을 조회합니다. " +
                    "회원인 경우 회원 정보(나이, 연령대, 성별)와 전체 기기의 count 합산을 반환하고, " +
                    "비회원인 경우 기기 정보와 해당 기기의 count만 반환합니다."
    )
    @GetMapping("/info")
    public ResponseEntity<ResMonitoringInfoDto> getMonitoringInfo(
            @Parameter(description = "기기 고유 번호", required = true, example = "device_unique_identifier_12345")
            @RequestParam String deviceNumber) {
        ResMonitoringInfoDto response = monitoringService.getMonitoringInfo(deviceNumber);
        return ResponseEntity.ok(response);
    }
}