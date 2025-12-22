package com.application.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OnboardingDto {

    @NotBlank(message = "성별 정보는 필수입니다.")
    private String gender; // "male", "female", "none"

    @NotBlank(message = "연령대 정보는 필수입니다.")
    private String ageRange; // "under_19", "20_24", "25_29", "30_34", "35_39", "50_over"
}