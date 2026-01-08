package com.application.domain.member.controller;

import com.application.common.Constant;
import com.application.common.auth.dto.oauth2Dto.CustomOAuth2User;
import com.application.common.exception.custom.CustomApiException;
import com.application.common.response.ResponseDto;
import com.application.domain.member.dto.MemberDto;
import com.application.domain.member.dto.MemberUpdateDto;
import com.application.domain.member.entity.Member;

import com.application.domain.member.service.MemberService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v2/members")
@Tag(name = "회원 관련 API", description = "회원 정보 조회, 수정, 탈퇴 및 프로필 관리")
public class MemberV2Controller implements MemberV2ControllerDocs {

    private final MemberService memberService;

    @Override
    @GetMapping("/get/member")
    public ResponseEntity<?> getMember(@AuthenticationPrincipal CustomOAuth2User customOAuth2User){
        Member member = memberService.getMemberByCredentialId(customOAuth2User.getCredentialId());
        MemberDto memberDto = MemberDto.from(member);
        return new ResponseEntity<>(new ResponseDto<>(Constant.SUCCESS_CODE, "Get Member Info", memberDto), HttpStatus.OK);
    }

//    @Operation(summary = "회원 업데이트")
    @Override
    @PostMapping("/update/member")
    public ResponseEntity<?> updateMember(@AuthenticationPrincipal CustomOAuth2User customOAuth2User,
                                          @Valid @RequestBody MemberUpdateDto updateMemberDto,
                                          BindingResult bindingResult){
        Member member = memberService.getMemberByCredentialId(customOAuth2User.getCredentialId());
        memberService.updateMemberInfo(member, updateMemberDto);

        return new ResponseEntity<>(new ResponseDto<>(Constant.SUCCESS_CODE, "Update member", updateMemberDto), HttpStatus.OK);
    }

//    @Operation(summary = "회원 탈퇴")
    @Override
    @DeleteMapping("/delete/member")
    public ResponseEntity<?> deleteMember(@AuthenticationPrincipal CustomOAuth2User customOAuth2User){
        Long memberId = memberService.getMemberByCredentialId(customOAuth2User.getCredentialId()).getId();
        memberService.deleteMember(memberId);
        return new ResponseEntity<>(new ResponseDto<>(Constant.SUCCESS_CODE, "Delete Member", null), HttpStatus.OK);
    }

//    @Operation(summary = "회원 프로필사진 업로드")
    @Override
    @PostMapping("/upload/profile")
    public ResponseEntity<?> saveProfileImage(@AuthenticationPrincipal CustomOAuth2User customOAuth2User , MultipartFile file){
        if ( memberService.saveProfile(customOAuth2User.getCredentialId(), file) ){
            return new ResponseEntity<>(new ResponseDto<>(Constant.SUCCESS_CODE, "success upload profile", null), HttpStatus.OK);
        }else{
            throw new CustomApiException("Fail Upload Profile");
        }
    }

//    @Operation(summary = "회원 프로필사진 조회")
    @Override
    @GetMapping("/profile")
    public ResponseEntity<Resource> getProfile(@AuthenticationPrincipal CustomOAuth2User customOAuth2User){
        Map<String,Object> map = memberService.getProfile(customOAuth2User.getCredentialId());
        try {
            return ResponseEntity.ok()
                    .contentType((MediaType) map.get("ext"))
                    .body(new UrlResource((URI) map.get("uri")));
        }catch(MalformedURLException e){
            throw new RuntimeException("no find file");
        }
    }

}
