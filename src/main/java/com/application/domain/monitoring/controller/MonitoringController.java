package com.application.domain.monitoring.controller;

import com.application.common.Constant;
import com.application.common.response.ResponseDto;
import com.application.domain.monitoring.dto.response.SaveOnboardingReq;
import com.application.domain.monitoring.dto.response.TrackingReq;
import com.application.domain.monitoring.dto.request.MonitoringInfoRes;
import com.application.domain.monitoring.dto.response.OnboardingStatusRes;
import com.application.domain.monitoring.dto.request.TrackingRes;
import com.application.domain.monitoring.service.MonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public ResponseEntity<TrackingRes> trackPageAccess(@Valid @RequestBody TrackingReq reqTrackingDto) {
        TrackingRes response = monitoringService.trackPageAccess(reqTrackingDto);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "기기 정보 조회",
            description = "기기 고유 번호로 기기 정보, 연령, 성별을 조회합니다. " +
                    "회원인 경우 회원 정보(나이, 연령대, 성별)와 전체 기기의 count 합산을 반환하고, " +
                    "비회원인 경우 기기 정보와 해당 기기의 count만 반환합니다."
    )
    @GetMapping("/info")
    public ResponseEntity<MonitoringInfoRes> getMonitoringInfo(
            @Parameter(description = "기기 고유 번호", required = true, example = "device_unique_identifier_12345")
            @RequestParam String deviceNumber) {
        MonitoringInfoRes response = monitoringService.getMonitoringInfo(deviceNumber);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "온보딩 상태 확인",
            description = """
                    🔓 **인증 불필요** - 기기 고유 번호로 온보딩 완료 여부를 확인합니다.

                    스플래시 화면에서 이 API를 호출하여 온보딩 화면 표시 여부를 결정합니다.

                    **확인 로직:**
                    - 회원(로그인): Member 테이블의 gender, ageRange 필드로 확인
                    - 비회원(비로그인): Monitoring 테이블의 onboardingCompleted 필드로 확인
                    - 최초 접근: requiresOnboarding=true 반환

                    **Response:**
                    - onboardingCompleted: 온보딩 완료 여부
                    - requiresOnboarding: 온보딩이 필요한지 여부 (onboardingCompleted의 반대값)
                    - isMember: 회원 여부
                    """
    )
    @GetMapping("/onboarding/status")
    public ResponseEntity<OnboardingStatusRes> getOnboardingStatus(
            @Parameter(description = "기기 고유 번호", required = true, example = "device_unique_identifier_12345")
            @RequestParam String deviceNumber) {
        OnboardingStatusRes response = monitoringService.getOnboardingStatus(deviceNumber);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "비회원 온보딩 정보 저장",
            description = """
                    🔓 **인증 불필요** - 비로그인 사용자의 온보딩 정보를 저장합니다.

                    비회원(로그인 전) 사용자의 온보딩 정보를 Monitoring 테이블에 저장합니다.
                    나중에 로그인하면 이 정보가 자동으로 Member 테이블로 복사됩니다.

                    **로그인한 사용자는 `/api/v2/members/onboarding` API를 사용해야 합니다.**

                    **Request Body:**
                    - deviceNumber (필수): 기기 고유 번호
                    - gender (필수): 성별 정보
                    - ageRange (필수): 연령대 정보
                    """
    )
    @PostMapping("/onboarding")
    public ResponseEntity<?> saveNonMemberOnboarding(@Valid @RequestBody SaveOnboardingReq dto) {
        monitoringService.saveNonMemberOnboarding(dto);
        return new ResponseEntity<>(new ResponseDto<>(Constant.SUCCESS_CODE, "Save Non-Member Onboarding Info", dto), HttpStatus.OK);
    }
}