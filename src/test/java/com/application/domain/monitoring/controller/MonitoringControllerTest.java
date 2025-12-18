package com.application.domain.monitoring.controller;

import com.application.domain.monitoring.dto.ReqTrackingDto;
import com.application.domain.monitoring.repository.MonitoringRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MonitoringRepository monitoringRepository;

    @AfterEach
    void tearDown() {
        monitoringRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v2/monitoring/track - 최초 접근 시 성공 응답을 반환해야 한다")
    void trackPageAccess_FirstAccess_ShouldReturnSuccess() throws Exception {
        // given
        ReqTrackingDto request = new ReqTrackingDto();
        request.setDeviceNumber("api_test_device_001");
        request.setCount(1L);

        // when & then
        mockMvc.perform(post("/api/v2/monitoring/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceNumber").value("api_test_device_001"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.isFirstAccess").value(true))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v2/monitoring/track - 기존 deviceNumber로 접근 시 count가 업데이트되어야 한다")
    void trackPageAccess_ExistingDevice_ShouldUpdateCount() throws Exception {
        // given
        String deviceNumber = "api_test_device_002";

        // 최초 접근
        ReqTrackingDto firstRequest = new ReqTrackingDto();
        firstRequest.setDeviceNumber(deviceNumber);
        firstRequest.setCount(1L);

        mockMvc.perform(post("/api/v2/monitoring/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isOk());

        // when & then - 2번째 접근
        ReqTrackingDto secondRequest = new ReqTrackingDto();
        secondRequest.setDeviceNumber(deviceNumber);
        secondRequest.setCount(2L);

        mockMvc.perform(post("/api/v2/monitoring/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceNumber").value(deviceNumber))
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.isFirstAccess").value(false))
                .andExpect(jsonPath("$.createdAt").value(notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v2/monitoring/track - deviceNumber가 없으면 400 에러를 반환해야 한다")
    void trackPageAccess_MissingDeviceNumber_ShouldReturnBadRequest() throws Exception {
        // given
        ReqTrackingDto request = new ReqTrackingDto();
        request.setCount(1L);
        // deviceNumber는 설정하지 않음

        // when & then
        mockMvc.perform(post("/api/v2/monitoring/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v2/monitoring/track - count가 없으면 400 에러를 반환해야 한다")
    void trackPageAccess_MissingCount_ShouldReturnBadRequest() throws Exception {
        // given
        ReqTrackingDto request = new ReqTrackingDto();
        request.setDeviceNumber("api_test_device_003");
        // count는 설정하지 않음

        // when & then
        mockMvc.perform(post("/api/v2/monitoring/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v2/monitoring/track - 연속으로 여러 번 접근해도 정상 동작해야 한다")
    void trackPageAccess_MultipleSequentialAccess_ShouldWork() throws Exception {
        // given
        String deviceNumber = "api_test_device_004";

        // when & then - 5번 연속 접근
        for (int i = 1; i <= 5; i++) {
            ReqTrackingDto request = new ReqTrackingDto();
            request.setDeviceNumber(deviceNumber);
            request.setCount((long) i);

            mockMvc.perform(post("/api/v2/monitoring/track")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.deviceNumber").value(deviceNumber))
                    .andExpect(jsonPath("$.count").value(i))
                    .andExpect(jsonPath("$.isFirstAccess").value(i == 1));
        }
    }

    @Test
    @DisplayName("POST /api/v2/monitoring/track - 서로 다른 deviceNumber는 독립적으로 관리되어야 한다")
    void trackPageAccess_DifferentDevices_ShouldBeIndependent() throws Exception {
        // given
        String device1 = "api_test_device_005";
        String device2 = "api_test_device_006";

        // when & then - device1 접근
        ReqTrackingDto request1 = new ReqTrackingDto();
        request1.setDeviceNumber(device1);
        request1.setCount(3L);

        mockMvc.perform(post("/api/v2/monitoring/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));

        // device2 접근 (최초)
        ReqTrackingDto request2 = new ReqTrackingDto();
        request2.setDeviceNumber(device2);
        request2.setCount(1L);

        mockMvc.perform(post("/api/v2/monitoring/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.isFirstAccess").value(true));

        // device1 다시 접근
        request1.setCount(5L);
        mockMvc.perform(post("/api/v2/monitoring/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5))
                .andExpect(jsonPath("$.isFirstAccess").value(false));
    }
}