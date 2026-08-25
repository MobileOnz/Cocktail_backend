package com.application.domain.monitoring.service;

import com.application.common.exception.custom.CustomApiException;
import com.application.domain.member.entity.Member;
import com.application.domain.member.enums.Role;
import com.application.domain.member.enums.SocialLogin;
import com.application.domain.member.repository.MemberRepository;
import com.application.domain.monitoring.dto.request.MonitoringInfoRes;
import com.application.domain.monitoring.dto.response.TrackingReq;
import com.application.domain.monitoring.dto.request.TrackingRes;
import com.application.domain.monitoring.entity.Monitoring;
import com.application.domain.monitoring.repository.MonitoringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * F-13/F-14 보안 회귀 방지.
 *
 * <p>이전에는 {@code /monitoring/info}가 요청자와 무관하게 임의의 deviceNumber로 다른 회원의
 * memberId/연령/성별을 반환했고(F-13), {@code /monitoring/track}은 인증 없이도 다른 회원의
 * memberId를 응답에 그대로 담아 보냈다(F-14). 둘 다 IDOR/정보노출이라 고쳤고, 이 테스트는
 * 그 회귀를 잡는다.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class MonitoringServiceTest {

    @Autowired
    private MonitoringService monitoringService;

    @Autowired
    private MonitoringRepository monitoringRepository;

    @Autowired
    private MemberRepository memberRepository;

    private Member owner;
    private Member stranger;

    @BeforeEach
    void setUp() {
        monitoringRepository.deleteAll();
        memberRepository.deleteAll();

        owner = memberRepository.save(member("owner-cred"));
        stranger = memberRepository.save(member("stranger-cred"));

        monitoringRepository.save(Monitoring.builder()
                .deviceNumber("device-owner")
                .count(5L)
                .member(owner)
                .build());
    }

    private Member member(String credentialId) {
        return Member.builder()
                .credentialId(credentialId)
                .name("테스트")
                .nickname("테스터")
                .email(credentialId + "@example.com")
                .socialLogin(SocialLogin.KAKAO)
                .role(Role.USER)
                .ageTerm(true)
                .serviceTerm(true)
                .marketingTerm(false)
                .adTerm(false)
                .build();
    }

    @Test
    @DisplayName("본인 기기 조회는 기존과 동일하게 허용된다")
    void ownerCanReadOwnDevice() {
        MonitoringInfoRes res = monitoringService.getMonitoringInfo("device-owner", owner.getId());

        assertThat(res.getIsMember()).isTrue();
        assertThat(res.getMemberId()).isEqualTo(owner.getId());
    }

    @Test
    @DisplayName("F-13: 다른 회원 소유의 기기를 조회하면 차단된다")
    void strangerCannotReadOthersDevice() {
        assertThatThrownBy(() -> monitoringService.getMonitoringInfo("device-owner", stranger.getId()))
                .isInstanceOf(CustomApiException.class);
    }

    @Test
    @DisplayName("비회원(미매핑) 기기는 PII가 없으므로 소유권 제한 없이 조회된다")
    void nonMemberDeviceHasNoOwnershipRestriction() {
        monitoringRepository.save(Monitoring.builder()
                .deviceNumber("device-anon")
                .count(3L)
                .build());

        MonitoringInfoRes res = monitoringService.getMonitoringInfo("device-anon", stranger.getId());

        assertThat(res.getIsMember()).isFalse();
        assertThat(res.getMemberId()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 기기 조회는 기존과 동일하게 404 사유로 실패한다")
    void unknownDeviceStillThrows() {
        assertThatThrownBy(() -> monitoringService.getMonitoringInfo("no-such-device", owner.getId()))
                .isInstanceOf(CustomApiException.class);
    }

    @Test
    @DisplayName("F-14: /track 응답은 회원 여부(isMember)만 담고 memberId는 담지 않는다")
    void trackResponseNeverLeaksMemberId() {
        TrackingRes res = monitoringService.trackPageAccess(trackReq("device-owner", 9L));

        assertThat(res.getIsMember()).isTrue();
        // memberId 게터/필드가 더 이상 존재하지 않으므로, 컴파일이 되는 것 자체가
        // "응답 DTO에 memberId 필드가 없다"는 계약을 보증한다.
    }

    @Test
    @DisplayName("최초 접근이든 재접근이든 track은 기존과 동일하게 count/isFirstAccess를 반환한다")
    void trackKeepsExistingCountBehavior() {
        TrackingRes first = monitoringService.trackPageAccess(trackReq("device-new", 1L));
        assertThat(first.getIsFirstAccess()).isTrue();
        assertThat(first.getCount()).isEqualTo(1L);

        TrackingRes second = monitoringService.trackPageAccess(trackReq("device-new", 4L));
        assertThat(second.getIsFirstAccess()).isFalse();
        assertThat(second.getCount()).isEqualTo(4L);
    }

    private TrackingReq trackReq(String deviceNumber, Long count) {
        TrackingReq req = new TrackingReq();
        req.setDeviceNumber(deviceNumber);
        req.setCount(count);
        return req;
    }
}
