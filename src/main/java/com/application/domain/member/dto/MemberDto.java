package com.application.domain.member.dto;

import com.application.domain.member.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
public class MemberDto {
    private Long id;
    private String credentialId;
    private String name;
    private String nickname;
    private String email;
    private String gender;
    private String addr;
    private Integer age;
    private String profile;
    private String phone;
    private String role;
    private Boolean ageTerm;
    private Boolean serviceTerm;
    private Boolean marketingTerm;
    private Boolean adTerm;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MemberDto from(Member member) {
        return MemberDto.builder()
                .id(member.getId())
                .credentialId(member.getCredentialId())
                .name(member.getName())
                .nickname(member.getNickname())
                .email(member.getEmail())
                .gender(
                        member.getGender() != null ? member.getGender().getEnglish() : null
                )
                .addr(member.getAddr())
                .age(member.getAge())
                .profile(member.getProfile())
                .phone(member.getPhone())
                .role(member.getRole())
                .ageTerm(member.getAgeTerm() != null ? member.getAgeTerm() : true)
                .serviceTerm(member.getServiceTerm() != null ? member.getServiceTerm() : true)
                .marketingTerm(member.getMarketingTerm() != null ? member.getMarketingTerm() : true)
                .adTerm(member.getAdTerm() != null ? member.getAdTerm() : true)
                .createdAt(member.getCreatedAt())
                .updatedAt(member.getUpdatedAt())
                .build();
    }
}

