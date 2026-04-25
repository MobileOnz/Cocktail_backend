package com.application.domain.inquiry.controller;

import com.application.common.auth.dto.oauth2Dto.CustomOAuth2User;
import com.application.common.exception.custom.CustomApiException;
import com.application.common.response.ResponseDto;
import com.application.domain.inquiry.dto.request.InquiryCreateReq;
import com.application.domain.inquiry.dto.response.InquiryCreateRes;
import com.application.domain.inquiry.dto.response.InquiryPageRes;
import com.application.domain.inquiry.dto.response.InquiryRes;
import com.application.domain.inquiry.service.InquiryService;
import com.application.domain.member.entity.Member;
import com.application.domain.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v2/inquiry")
@RequiredArgsConstructor
@Tag(name = "1:1 문의", description = "사용자 1:1 문의 등록 / 조회 API")
public class InquiryController {

    private final InquiryService inquiryService;
    private final MemberService memberService;

    @Operation(
            summary = "1:1 문의 등록",
            description = "로그인 사용자 / 비로그인 사용자 모두 사용 가능합니다. " +
                    "비로그인 시 device_number(body 또는 X-Device-Number 헤더) 필수."
    )
    @PostMapping
    public ResponseEntity<ResponseDto<InquiryCreateRes>> create(
            @Valid @RequestBody InquiryCreateReq req,
            @RequestHeader(value = "X-Device-Number", required = false) String deviceHeader
    ) {
        Member member = resolveMemberOrNull();
        InquiryCreateRes res = inquiryService.create(req, member, deviceHeader);
        return ResponseEntity.ok(ResponseDto.onSuccess("문의가 접수되었습니다.", res));
    }

    @Operation(
            summary = "내 1:1 문의 목록 조회",
            description = "인증 필요. 본인이 등록한 문의만 페이지네이션으로 반환합니다."
    )
    @GetMapping("/mine")
    public ResponseEntity<ResponseDto<InquiryPageRes>> listMine(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Member member = resolveMemberOrThrow();
        InquiryPageRes res = inquiryService.listMine(member.getId(), pageable);
        return ResponseEntity.ok(ResponseDto.onSuccess(res));
    }

    @Operation(
            summary = "1:1 문의 단건 조회",
            description = "인증 필요. 본인 문의 또는 ADMIN만 조회 가능합니다."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ResponseDto<InquiryRes>> getOne(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomOAuth2User principal)) {
            throw new CustomApiException("인증이 필요합니다.");
        }
        String role = principal.getAuthorities().stream()
                .findFirst().map(a -> a.getAuthority()).orElse("USER");
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);

        Member member = memberService.getMemberByCredentialId(principal.getCredentialId());
        Long memberId = member != null ? member.getId() : null;

        InquiryRes res = inquiryService.getOneForMemberOrAdmin(id, memberId, isAdmin);
        return ResponseEntity.ok(ResponseDto.onSuccess(res));
    }

    // ------------------------------------------------------------------
    // private helpers
    // ------------------------------------------------------------------

    private Member resolveMemberOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomOAuth2User principal)) {
            return null;
        }
        try {
            return memberService.getMemberByCredentialId(principal.getCredentialId());
        } catch (Exception e) {
            log.warn("[INQUIRY] resolveMemberOrNull failed: {}", e.getMessage());
            return null;
        }
    }

    private Member resolveMemberOrThrow() {
        Member member = resolveMemberOrNull();
        if (member == null) {
            throw new CustomApiException("인증이 필요합니다.");
        }
        return member;
    }
}
