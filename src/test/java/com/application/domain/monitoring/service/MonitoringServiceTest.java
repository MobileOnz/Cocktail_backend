package com.application.domain.monitoring.service;

import com.application.domain.member.entity.Member;
import com.application.domain.member.enums.Role;
import com.application.domain.member.enums.SocialLogin;
import com.application.domain.member.repository.MemberRepository;
import com.application.domain.monitoring.dto.ReqTrackingDto;
import com.application.domain.monitoring.dto.ResTrackingDto;
import com.application.domain.monitoring.entity.Monitoring;
import com.application.domain.monitoring.repository.MonitoringRepository;
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
class MonitoringServiceTest {

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
    @DisplayName("최초 페이지 접근 시 Monitoring 레코드가 생성되어야 한다")
    void trackPageAccess_FirstAccess_ShouldCreateRecord() {
        // given
        String deviceNumber = "test_device_001";
        ReqTrackingDto request = new ReqTrackingDto();
        request.setDeviceNumber(deviceNumber);
        request.setCount(1L);

        // when
        ResTrackingDto response = monitoringService.trackPageAccess(request);

        // then
        assertThat(response.getDeviceNumber()).isEqualTo(deviceNumber);
        assertThat(response.getCount()).isEqualTo(1L);
        assertThat(response.getIsFirstAccess()).isTrue();
        assertThat(response.getCreatedAt()).isNotNull();

        // DB 확인
        Optional<Monitoring> monitoring = monitoringRepository.findByDeviceNumber(deviceNumber);
        assertThat(monitoring).isPresent();
        assertThat(monitoring.get().getCount()).isEqualTo(1L);
        assertThat(monitoring.get().getMember()).isNull();
    }

    @Test
    @DisplayName("기존 deviceNumber로 접근 시 count가 업데이트되어야 한다")
    void trackPageAccess_ExistingDevice_ShouldUpdateCount() {
        // given
        String deviceNumber = "test_device_002";

        // 최초 접근
        ReqTrackingDto firstRequest = new ReqTrackingDto();
        firstRequest.setDeviceNumber(deviceNumber);
        firstRequest.setCount(1L);
        ResTrackingDto firstResponse = monitoringService.trackPageAccess(firstRequest);

        // when - 2번째 접근
        ReqTrackingDto secondRequest = new ReqTrackingDto();
        secondRequest.setDeviceNumber(deviceNumber);
        secondRequest.setCount(2L);
        ResTrackingDto secondResponse = monitoringService.trackPageAccess(secondRequest);

        // then
        assertThat(secondResponse.getDeviceNumber()).isEqualTo(deviceNumber);
        assertThat(secondResponse.getCount()).isEqualTo(2L);
        assertThat(secondResponse.getIsFirstAccess()).isFalse();
        assertThat(secondResponse.getCreatedAt()).isEqualTo(firstResponse.getCreatedAt()); // 생성 시간은 동일

        // DB 확인
        Optional<Monitoring> monitoring = monitoringRepository.findByDeviceNumber(deviceNumber);
        assertThat(monitoring).isPresent();
        assertThat(monitoring.get().getCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("기존 모니터링 데이터가 있는 경우 회원과 매핑되어야 한다")
    @Transactional
    void mapToMember_ExistingMonitoring_ShouldMapToMember() {
        // given
        String deviceNumber = "test_device_003";

        // 비로그인 상태에서 페이지 접근 (모니터링 데이터 생성)
        Monitoring monitoring = Monitoring.builder()
                .deviceNumber(deviceNumber)
                .count(5L)
                .build();
        monitoringRepository.save(monitoring);

        // 회원 생성
        Member member = Member.builder()
                .credentialId("test_credential_001")
                .name("테스트 사용자")
                .nickname("테스터")
                .email("test@example.com")
                .socialLogin(SocialLogin.KAKAO)
                .role(Role.USER)
                .ageTerm(true)
                .serviceTerm(true)
                .marketingTerm(false)
                .adTerm(false)
                .build();
        Member savedMember = memberRepository.save(member);

        // when
        monitoringService.mapToMember(deviceNumber, savedMember);

        // then
        Optional<Monitoring> result = monitoringRepository.findByDeviceNumber(deviceNumber);
        assertThat(result).isPresent();
        assertThat(result.get().getMember()).isNotNull();
        assertThat(result.get().getMember().getId()).isEqualTo(savedMember.getId());
        assertThat(result.get().getCount()).isEqualTo(5L); // count는 유지
    }

    @Test
    @DisplayName("기존 모니터링 데이터가 없는 경우 새로 생성하여 회원과 매핑되어야 한다")
    @Transactional
    void mapToMember_NoExistingMonitoring_ShouldCreateAndMap() {
        // given
        String deviceNumber = "test_device_004";

        // 회원 생성
        Member member = Member.builder()
                .credentialId("test_credential_002")
                .name("테스트 사용자2")
                .nickname("테스터2")
                .email("test2@example.com")
                .socialLogin(SocialLogin.NAVER)
                .role(Role.USER)
                .ageTerm(true)
                .serviceTerm(true)
                .marketingTerm(false)
                .adTerm(false)
                .build();
        Member savedMember = memberRepository.save(member);

        // when
        monitoringService.mapToMember(deviceNumber, savedMember);

        // then
        Optional<Monitoring> result = monitoringRepository.findByDeviceNumber(deviceNumber);
        assertThat(result).isPresent();
        assertThat(result.get().getMember()).isNotNull();
        assertThat(result.get().getMember().getId()).isEqualTo(savedMember.getId());
        assertThat(result.get().getCount()).isEqualTo(0L); // 새로 생성되므로 count는 0
    }

    @Test
    @DisplayName("deviceNumber가 null이면 매핑 작업을 수행하지 않아야 한다")
    @Transactional
    void mapToMember_NullDeviceNumber_ShouldDoNothing() {
        // given
        Member member = Member.builder()
                .credentialId("test_credential_003")
                .name("테스트 사용자3")
                .nickname("테스터3")
                .email("test3@example.com")
                .socialLogin(SocialLogin.APPLE)
                .role(Role.USER)
                .ageTerm(true)
                .serviceTerm(true)
                .marketingTerm(false)
                .adTerm(false)
                .build();
        Member savedMember = memberRepository.save(member);

        // when
        monitoringService.mapToMember(null, savedMember);

        // then
        long count = monitoringRepository.count();
        assertThat(count).isEqualTo(0); // 아무것도 생성되지 않아야 함
    }

    @Test
    @DisplayName("여러 번 페이지 접근 시 count가 순차적으로 증가해야 한다")
    void trackPageAccess_MultipleAccess_ShouldIncrementCount() {
        // given
        String deviceNumber = "test_device_005";

        // when & then
        for (int i = 1; i <= 10; i++) {
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

        // 최종 확인
        Optional<Monitoring> monitoring = monitoringRepository.findByDeviceNumber(deviceNumber);
        assertThat(monitoring).isPresent();
        assertThat(monitoring.get().getCount()).isEqualTo(10L);
    }
}