package com.application.domain.monitoring.entity;

import com.application.common.time.BaseTimeEntity;
import com.application.domain.member.entity.Member;
import com.application.domain.member.enums.AgeRange;
import com.application.domain.member.enums.Gender;
import jakarta.persistence.*;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Getter
@Setter
@NoArgsConstructor
@Entity(name="monitoring")
public class Monitoring extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="device_number", nullable = false, unique = true)
    private String deviceNumber;

    // 접근 횟수
    @Column(name="count", nullable = false)
    private Long count = 0L;

    @Column(name="onboarding_completed", nullable = false, columnDefinition = "boolean default false")
    private Boolean onboardingCompleted = false;

    @Column(name="gender")
    private Gender gender;

    @Column(name="age_range")
    private AgeRange ageRange;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Builder
    public Monitoring(String deviceNumber, Long count, Member member, Boolean onboardingCompleted, Gender gender, AgeRange ageRange) {
        this.deviceNumber = deviceNumber;
        this.count = count != null ? count : 0L;
        this.member = member;
        this.onboardingCompleted = onboardingCompleted != null ? onboardingCompleted : false;
        this.gender = gender;
        this.ageRange = ageRange;
    }

    /**
     * 접근 횟수 증가 (서버에서 count 관리 시 사용)
     * 현재는 프론트에서 count를 관리하므로 주석처리
     * 향후 서버 관리 방식으로 변경 시 주석 해제
     */
//    public void incrementCount() {
//        this.count++;
//    }

    /**
     * 접근 횟수 업데이트 (프론트에서 전달받은 count 값으로 설정)
     */
    public void updateCount(Long count) {
        this.count = count;
    }

    /**
     * 회원 매핑
     */
    public void mapToMember(Member member) {
        this.member = member;
    }

    /**
     * 온보딩 정보 저장 (비회원용)
     */
    public void saveOnboardingInfo(Gender gender, AgeRange ageRange) {
        this.gender = gender;
        this.ageRange = ageRange;
        this.onboardingCompleted = true;
    }

    /**
     * 온보딩 완료 상태로 설정 (회원 로그인 시 동기화용)
     */
    public void markOnboardingCompleted() {
        this.onboardingCompleted = true;
    }
}