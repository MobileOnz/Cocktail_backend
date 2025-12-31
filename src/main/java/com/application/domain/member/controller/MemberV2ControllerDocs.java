package com.application.domain.member.controller;

import com.application.common.auth.dto.oauth2Dto.CustomOAuth2User;
import com.application.common.response.ResponseDto;
import com.application.domain.member.dto.MemberUpdateDto;
import com.application.domain.member.dto.OnboardingDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

public interface MemberV2ControllerDocs {


    @Operation(summary = "회원 정보 조회", description = "현재 로그인한 사용자의 상세 정보를 조회")
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(example = """
                            {
                                "code": 1,
                                "msg": "Get Member Info",
                                "data": {
                                    "created_at": "2025-03-06T22:47:01.962904",
                                    "updated_at": "2025-03-06T22:47:01.962904",
                                    "id": 1,
                                    "creadential_id": "google114421399913729594456",
                                    "name": "박우영",
                                    "nickname": "박우영",
                                    "email": "wooyeong1998@gmail.com",
                                    "social_login" : "KAKAO",
                                    "gender": null,
                                    "addr": null,
                                    "age": null,
                                    "profile": null,
                                    "phone": "010-2222-3333",
                                    "role": "ROLE_USER",
                                    "ageTerm" : true/false, //14세미만 약관동의
                                       "serviceTerm" : true/false, // service 약관동의
                                       "marketingTerm" : true/false, // 마켓팅 약관동의
                                       "adTerm" : true/false //광고성 약관동의
                                }
                            }
            """))),

            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(example = """
                            {
                                "code": -1,
                                "msg": "Access Token 값이 헤더에 존재하지 않음",
                                "data": "null"
                            }
            """)))

    })
    ResponseEntity<?> getMember(@AuthenticationPrincipal CustomOAuth2User customOAuth2User);

    @Operation(summary = "회원 정보 수정", description = "현재 로그인한 사용자의 정보 수정")
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(example = """
                            {
                                "code": 1,
                                "msg": "Update member",
                                "data": {
                                    "gender": "male",
                                    "name": "test",
                                    "addr": "seoul",
                                    "age": 20,
                                    "nickName": "test"
                                }
                            }
            """))),

            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(example = """
                            {
                                "code": -1,
                                "msg": "유효성 검사 실패",
                                "data": {
                                    "gender": "Invalid gender. Must be 'male' or 'female' or 'none'"
                                }
                            }
            """)))

    })
    ResponseEntity<?> updateMember(@AuthenticationPrincipal CustomOAuth2User customOAuth2User,
                                   @Valid @RequestBody MemberUpdateDto updateMemberDto,
                                   BindingResult bindingResult);

    // 회원 탈퇴
    @Operation(summary = "회원 정보 조회", description = "현재 로그인한 회원을 탈퇴 처리")
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(example = """
                            {
                                "code": 1,
                                "msg": "Delete Member",
                                "data": null
                            }
            """))),

            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(example = """
                            {
                                "code": -1,
                                "msg": "Access Token 값이 헤더에 존재하지 않음",
                                "data": "null"
                            }
            """)))

    })
    ResponseEntity<?> deleteMember(@AuthenticationPrincipal CustomOAuth2User customOAuth2User);

    // 회원 프로필사진 업로드
    @Operation(summary = "회원 프로필 사진 업로드", description = "프로필 이미지를 업로드하여 저장")
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(example = """
                            {
                                "code": 1,
                                "msg": "success upload profile",
                                "data": null
                            }
            """))),

            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(example = """
                            {
                                "code": -1,
                                "msg": "Access Token 값이 헤더에 존재하지 않음",
                                "data": "null"
                            }
            """)))

    })
    ResponseEntity<?> saveProfileImage(@AuthenticationPrincipal CustomOAuth2User customOAuth2User , MultipartFile file);

    // 회원 프로필사진 조회
    @Operation(summary = "회원 프로필 사진 조회", description = "현재 로그인한 사용자의 상세 정보를 조회")
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(example = """
                            {
                                "code": 1,
                                "msg": "profile 이미지 정보 출력",
                                "data": [
                                    "profile 이미지 정보 출력"
                                ]
                            }
            """))),

            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(example = """
                            {
                                "code": -1,
                                "msg": "Access Token 값이 헤더에 존재하지 않음",
                                "data": "null"
                            }
            """)))

    })
    ResponseEntity<Resource> getProfile(@AuthenticationPrincipal CustomOAuth2User customOAuth2User);

    // 온보딩 정보 저장
    @Operation(
            summary = "회원 온보딩 정보 저장",
            description = """
                    🔒 **인증 필요** - 로그인한 사용자의 온보딩 정보를 저장합니다.

                    온보딩 화면에서 사용자의 성별과 연령대 정보를 Member 테이블에 저장하며,
                    해당 회원이 사용하는 모든 기기의 온보딩 상태를 자동으로 동기화합니다.

                    **비로그인 사용자는 `/api/v2/monitoring/onboarding` API를 사용해야 합니다.**

                    **Request Body:**
                    - gender (필수): 성별 정보
                    - ageRange (필수): 연령대 정보
                    - deviceNumber (선택): 사용하지 않음, 생략 가능
                    """
    )
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "저장 성공",
                    content = @Content(schema = @Schema(example = """
                            {
                                "code": 1,
                                "msg": "Save Onboarding Info",
                                "data": {
                                    "gender": "male",
                                    "ageRange": "20_24"
                                }
                            }
            """))),

            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(example = """
                            {
                                "code": -1,
                                "msg": "유효성 검사 실패",
                                "data": {
                                    "gender": "성별 정보는 필수입니다.",
                                    "ageRange": "연령대 정보는 필수입니다."
                                }
                            }
            """)))

    })
    ResponseEntity<?> saveOnboarding(@AuthenticationPrincipal CustomOAuth2User customOAuth2User,
                                      @Valid @RequestBody OnboardingDto onboardingDto,
                                      BindingResult bindingResult);
}
