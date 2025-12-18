package com.application.domain.monitoring;

import com.application.domain.member.entity.Member;
import com.application.domain.member.enums.Role;
import com.application.domain.member.enums.SocialLogin;
import com.application.domain.member.repository.MemberRepository;
import com.application.domain.monitoring.dto.ReqTrackingDto;
import com.application.domain.monitoring.dto.ResTrackingDto;
import com.application.domain.monitoring.entity.Monitoring;
import com.application.domain.monitoring.repository.MonitoringRepository;
import com.application.domain.monitoring.service.MonitoringService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MonitoringIntegrationTest {

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private MonitoringRepository monitoringRepository;

    @Autowired
    private MemberRepository memberRepository;

    @AfterEach
    void tearDown() {
        monitoringRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("[통합 시나리오] 비로그인 상태에서 페이지 접근 → 회원가입 → 모니터링 데이터 매핑 확인")
    @Transactional
    void fullScenario_GuestAccess_SignUp_ShouldMapMonitoringData() {
        // Step 1: 비로그인 상태에서 페이지 접근 (5번)
        String deviceNumber = "integration_test_device_001";

        for (int i = 1; i <= 5; i++) {
            ReqTrackingDto request = new ReqTrackingDto();
            request.setDeviceNumber(deviceNumber);
            request.setCount((long) i);

            ResTrackingDto response = monitoringService.trackPageAccess(request);

            assertThat(response.getCount()).isEqualTo((long) i);
            if (i == 1) {
                assertThat(response.getIsFirstAccess()).isTrue();
            } else {
                assertThat(response.getIsFirstAccess()).isFalse();
            }
        }

        // Step 1 검증: 모니터링 데이터가 생성되고 member는 null
        Optional<Monitoring> beforeSignup = monitoringRepository.findByDeviceNumber(deviceNumber);
        assertThat(beforeSignup).isPresent();
        assertThat(beforeSignup.get().getCount()).isEqualTo(5L);
        assertThat(beforeSignup.get().getMember()).isNull();

        // Step 2: 회원가입
        Member newMember = Member.builder()
                .credentialId("integration_test_credential")
                .name("통합테스트 사용자")
                .nickname("통테")
                .email("integration@test.com")
                .socialLogin(SocialLogin.KAKAO)
                .role(Role.USER)
                .ageTerm(true)
                .serviceTerm(true)
                .marketingTerm(true)
                .adTerm(false)
                .build();
        Member savedMember = memberRepository.save(newMember);

        // Step 3: 모니터링 데이터와 회원 매핑
        monitoringService.mapToMember(deviceNumber, savedMember);

        // Step 3 검증: 모니터링 데이터가 회원과 연결되고 count는 유지
        Optional<Monitoring> afterSignup = monitoringRepository.findByDeviceNumber(deviceNumber);
        assertThat(afterSignup).isPresent();
        assertThat(afterSignup.get().getCount()).isEqualTo(5L); // count는 그대로 유지
        assertThat(afterSignup.get().getMember()).isNotNull();
        assertThat(afterSignup.get().getMember().getId()).isEqualTo(savedMember.getId());
        assertThat(afterSignup.get().getMember().getCredentialId()).isEqualTo("integration_test_credential");

        // Step 4: 회원가입 후 추가 페이지 접근
        ReqTrackingDto afterSignupRequest = new ReqTrackingDto();
        afterSignupRequest.setDeviceNumber(deviceNumber);
        afterSignupRequest.setCount(6L);

        ResTrackingDto afterSignupResponse = monitoringService.trackPageAccess(afterSignupRequest);

        assertThat(afterSignupResponse.getCount()).isEqualTo(6L);
        assertThat(afterSignupResponse.getIsFirstAccess()).isFalse();

        // Step 4 검증: count가 증가하고 member 매핑은 유지
        Optional<Monitoring> finalCheck = monitoringRepository.findByDeviceNumber(deviceNumber);
        assertThat(finalCheck).isPresent();
        assertThat(finalCheck.get().getCount()).isEqualTo(6L);
        assertThat(finalCheck.get().getMember()).isNotNull();
        assertThat(finalCheck.get().getMember().getId()).isEqualTo(savedMember.getId());
    }

    @Test
    @DisplayName("[통합 시나리오] 페이지 접근 없이 바로 회원가입 시 count 0으로 모니터링 데이터 생성")
    @Transactional
    void fullScenario_DirectSignUp_ShouldCreateMonitoringWithZeroCount() {
        // Step 1: 회원가입 (페이지 접근 이력 없음)
        String deviceNumber = "integration_test_device_002";

        Member newMember = Member.builder()
                .credentialId("direct_signup_credential")
                .name("직접가입 사용자")
                .nickname("직가")
                .email("direct@test.com")
                .socialLogin(SocialLogin.NAVER)
                .role(Role.USER)
                .ageTerm(true)
                .serviceTerm(true)
                .marketingTerm(false)
                .adTerm(false)
                .build();
        Member savedMember = memberRepository.save(newMember);

        // Step 2: 모니터링 매핑 (기존 데이터 없음)
        monitoringService.mapToMember(deviceNumber, savedMember);

        // Step 2 검증: 새로운 모니터링 데이터 생성, count는 0
        Optional<Monitoring> monitoring = monitoringRepository.findByDeviceNumber(deviceNumber);
        assertThat(monitoring).isPresent();
        assertThat(monitoring.get().getCount()).isEqualTo(0L);
        assertThat(monitoring.get().getMember()).isNotNull();
        assertThat(monitoring.get().getMember().getId()).isEqualTo(savedMember.getId());

        // Step 3: 회원가입 후 첫 페이지 접근
        ReqTrackingDto firstAccessRequest = new ReqTrackingDto();
        firstAccessRequest.setDeviceNumber(deviceNumber);
        firstAccessRequest.setCount(1L);

        ResTrackingDto firstAccessResponse = monitoringService.trackPageAccess(firstAccessRequest);

        assertThat(firstAccessResponse.getCount()).isEqualTo(1L);
        assertThat(firstAccessResponse.getIsFirstAccess()).isFalse(); // 이미 레코드가 존재하므로 false

        // Step 3 검증
        Optional<Monitoring> afterAccess = monitoringRepository.findByDeviceNumber(deviceNumber);
        assertThat(afterAccess).isPresent();
        assertThat(afterAccess.get().getCount()).isEqualTo(1L);
        assertThat(afterAccess.get().getMember()).isNotNull();
    }

    @Test
    @DisplayName("[통합 시나리오] 여러 사용자가 각각 다른 deviceNumber로 독립적으로 동작")
    @Transactional
    void fullScenario_MultipleUsers_ShouldWorkIndependently() {
        // User 1: 비로그인 → 3번 접근 → 회원가입
        String device1 = "multi_user_device_001";
        for (int i = 1; i <= 3; i++) {
            ReqTrackingDto request = new ReqTrackingDto();
            request.setDeviceNumber(device1);
            request.setCount((long) i);
            monitoringService.trackPageAccess(request);
        }

        Member member1 = Member.builder()
                .credentialId("multi_user_1")
                .name("사용자1")
                .nickname("유저1")
                .email("user1@test.com")
                .socialLogin(SocialLogin.KAKAO)
                .role(Role.USER)
                .ageTerm(true)
                .serviceTerm(true)
                .build();
        Member savedMember1 = memberRepository.save(member1);
        monitoringService.mapToMember(device1, savedMember1);

        // User 2: 비로그인 → 7번 접근 → 회원가입
        String device2 = "multi_user_device_002";
        for (int i = 1; i <= 7; i++) {
            ReqTrackingDto request = new ReqTrackingDto();
            request.setDeviceNumber(device2);
            request.setCount((long) i);
            monitoringService.trackPageAccess(request);
        }

        Member member2 = Member.builder()
                .credentialId("multi_user_2")
                .name("사용자2")
                .nickname("유저2")
                .email("user2@test.com")
                .socialLogin(SocialLogin.NAVER)
                .role(Role.USER)
                .ageTerm(true)
                .serviceTerm(true)
                .build();
        Member savedMember2 = memberRepository.save(member2);
        monitoringService.mapToMember(device2, savedMember2);

        // 검증: 각 사용자의 데이터가 독립적으로 관리됨
        Optional<Monitoring> monitoring1 = monitoringRepository.findByDeviceNumber(device1);
        assertThat(monitoring1).isPresent();
        assertThat(monitoring1.get().getCount()).isEqualTo(3L);
        assertThat(monitoring1.get().getMember().getId()).isEqualTo(savedMember1.getId());

        Optional<Monitoring> monitoring2 = monitoringRepository.findByDeviceNumber(device2);
        assertThat(monitoring2).isPresent();
        assertThat(monitoring2.get().getCount()).isEqualTo(7L);
        assertThat(monitoring2.get().getMember().getId()).isEqualTo(savedMember2.getId());

        // 전체 레코드 개수 확인
        long totalCount = monitoringRepository.count();
        assertThat(totalCount).isEqualTo(2L);
    }
}