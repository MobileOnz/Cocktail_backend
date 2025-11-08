package com.application.web.controiler.member;

import com.application.common.Constant;
import com.application.common.auth.dto.oauth2Dto.CustomOAuth2User;
import com.application.common.exception.custom.CustomApiException;
import com.application.common.response.ResponseDto;
import com.application.domain.member.dto.MemberDto;
import com.application.domain.member.dto.MemberUpdateDto;
import com.application.domain.member.entity.Member;
import com.application.web.services.member.MemberService;
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
@RequestMapping("/api")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/get/member")
    public ResponseEntity<?> getMember(@AuthenticationPrincipal CustomOAuth2User customOAuth2User){
        Member member = memberService.getMemberByCredentialId(customOAuth2User.getCredentialId());
        MemberDto memberDto = MemberDto.from(member);
        return new ResponseEntity<>(new ResponseDto<>(Constant.SUCCESS_CODE, "Get Member Info", memberDto), HttpStatus.OK);
    }

    @PostMapping("/update/member")
    public ResponseEntity<?> updateMember(@AuthenticationPrincipal CustomOAuth2User customOAuth2User,
                                          @Valid @RequestBody MemberUpdateDto updateMemberDto,
                                          BindingResult bindingResult){
        Member member = memberService.getMemberByCredentialId(customOAuth2User.getCredentialId());
        memberService.updateMemberInfo(member, updateMemberDto);

        return new ResponseEntity<>(new ResponseDto<>(Constant.SUCCESS_CODE, "Update member", updateMemberDto), HttpStatus.OK);
    }

    @DeleteMapping("/delete/member")
    public ResponseEntity<?> deleteMember(@AuthenticationPrincipal CustomOAuth2User customOAuth2User){
        Long memberId = memberService.getMemberByCredentialId(customOAuth2User.getCredentialId()).getId();
        memberService.deleteMember(memberId);
        return new ResponseEntity<>(new ResponseDto<>(Constant.SUCCESS_CODE, "Delete Member", null), HttpStatus.OK);
    }


    @PostMapping("/upload/profile")
    public ResponseEntity<?> saveProfileImage(@AuthenticationPrincipal CustomOAuth2User customOAuth2User , MultipartFile file){
        if ( memberService.saveProfile(customOAuth2User.getCredentialId(), file) ){
            return new ResponseEntity<>(new ResponseDto<>(Constant.SUCCESS_CODE, "success upload profile", null), HttpStatus.OK);
        }else{
            throw new CustomApiException("Fail Upload Profile");
        }
    }

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
