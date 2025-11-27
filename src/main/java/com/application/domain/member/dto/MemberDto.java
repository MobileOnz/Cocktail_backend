package com.application.domain.member.dto;

import com.application.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "회원 상세 정보 응답 데이터 모델")
public class MemberDto {

    @Schema(description = "회원 고유 ID", example = "1")
    private Long id;

    @Schema(description = "로그인 시 사용되는 자격 증명 ID", example = "user1234")
    private String credentialId;

    @Schema(description = "회원 실명", example = "홍길동")
    private String name;

    @Schema(description = "회원 닉네임", example = "길동쓰")
    private String nickname;

    @Schema(description = "회원 이메일", example = "gildong@example.com")
    private String email;

    @Schema(description = "성별", example = "MALE") // Gender Enum의 English 값을 따른다고 가정
    private String gender;

    @Schema(description = "주소", example = "서울시 강남구")
    private String addr;

    @Schema(description = "나이", example = "30")
    private Integer age;

    @Schema(description = "프로필 이미지 URL", example = "http://example.com/profile/1.jpg")
    private String profile;

    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phone;

    @Schema(description = "회원 역할/권한", example = "USER")
    private String role;

    @Schema(description = "만 14세 이상 약관 동의 여부", example = "true")
    private Boolean ageTerm;

    @Schema(description = "서비스 이용 약관 동의 여부", example = "true")
    private Boolean serviceTerm;

    @Schema(description = "마케팅 정보 수신 동의 여부", example = "false")
    private Boolean marketingTerm;

    @Schema(description = "광고 정보 수신 동의 여부", example = "false")
    private Boolean adTerm;

    @Schema(description = "생성 일시", example = "2023-10-26T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "최종 수정 일시", example = "2023-10-26T10:00:00")
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

