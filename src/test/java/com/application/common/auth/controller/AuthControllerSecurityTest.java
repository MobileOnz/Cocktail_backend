package com.application.common.auth.controller;

import com.application.common.auth.OAuth2Service;
import com.application.common.auth.dto.login.ReqSocialLoginDto;
import com.application.common.auth.dto.login.ResTokenDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OAuth2Service oAuth2Service;

    @Test
    @DisplayName("SDK 소셜 로그인 API는 인증 없이 호출할 수 있다")
    void socialLogin_WithoutAuthorizationHeader_ReturnsOk() throws Exception {
        when(oAuth2Service.socialLogin(any(ReqSocialLoginDto.class)))
                .thenReturn(new ResTokenDto("access-token", "refresh-token"));

        mockMvc.perform(post("/api/v2/auth/social-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "kakao",
                                  "accessToken": "sdk-access-token",
                                  "deviceNumber": "device-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("token"));

        ArgumentCaptor<ReqSocialLoginDto> captor = ArgumentCaptor.forClass(ReqSocialLoginDto.class);
        verify(oAuth2Service).socialLogin(captor.capture());
        assertThat(captor.getValue().getProvider()).isEqualTo("kakao");
        assertThat(captor.getValue().getAccessToken()).isEqualTo("sdk-access-token");
        assertThat(captor.getValue().getDeviceNumber()).isEqualTo("device-001");
    }

    @Test
    @DisplayName("SDK 소셜 로그인 API는 잘못된 Authorization 헤더가 있어도 익명 요청으로 처리된다")
    void socialLogin_WithInvalidAuthorizationHeader_ReturnsOk() throws Exception {
        when(oAuth2Service.socialLogin(any(ReqSocialLoginDto.class)))
                .thenReturn(new ResTokenDto("access-token", "refresh-token"));

        mockMvc.perform(post("/api/v2/auth/social-login")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "naver",
                                  "accessToken": "sdk-access-token",
                                  "deviceNumber": "device-002"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("token"));
    }
}
