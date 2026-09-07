package com.application.domain.monitoring.controller;

import com.application.common.auth.jwt.JWTUtil;
import com.application.domain.member.entity.Member;
import com.application.domain.member.enums.Role;
import com.application.domain.member.enums.SocialLogin;
import com.application.domain.member.repository.MemberRepository;
import com.application.domain.monitoring.entity.Monitoring;
import com.application.domain.monitoring.repository.MonitoringRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-13 회귀 방지(HTTP 계층). {@code /api/v2/monitoring/info}가
 * (1) 인증 없이는 막히고, (2) 인증되어도 남의 기기는 못 보고, (3) 본인 기기는 그대로 보이는지 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("모니터링 컨트롤러 테스트")
class MonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MonitoringRepository monitoringRepository;

    @Autowired
    private JWTUtil jwtUtil;

    private Member owner;
    private String ownerToken;
    private String strangerToken;

    @BeforeEach
    void setUp() {
        owner = memberRepository.save(member("owner-cred"));
        Member stranger = memberRepository.save(member("stranger-cred"));

        monitoringRepository.save(Monitoring.builder()
                .deviceNumber("device-owner")
                .count(5L)
                .member(owner)
                .build());

        ownerToken = jwtUtil.createAccessJwt("owner-session", owner.getCredentialId(), owner.getRole());
        strangerToken = jwtUtil.createAccessJwt("stranger-session", stranger.getCredentialId(), stranger.getRole());
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
    @DisplayName("본인 기기 조회는 200과 본인 정보를 반환한다")
    void ownerCanReadOwnDevice() throws Exception {
        mockMvc.perform(get("/api/v2/monitoring/info")
                        .param("deviceNumber", "device-owner")
                        .header("Authorization", "Bearer " + ownerToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMember").value(true))
                .andExpect(jsonPath("$.memberId").value(owner.getId()));
    }

    @Test
    @DisplayName("F-13: 다른 회원의 기기를 조회하면 400으로 거절된다")
    void strangerCannotReadOthersDevice() throws Exception {
        mockMvc.perform(get("/api/v2/monitoring/info")
                        .param("deviceNumber", "device-owner")
                        .header("Authorization", "Bearer " + strangerToken))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(-1));
    }

    @Test
    @DisplayName("인증 없이 조회하면 401로 거절된다")
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/v2/monitoring/info")
                        .param("deviceNumber", "device-owner"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
}
