package com.application.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.application.common.logging.TraceIdContext;
import com.application.common.response.ResponseDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;

@DisplayName("전역 예외 응답 TRACE_ID")
class CustomExceptionHandlerTraceIdTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("예상하지 못한 오류 응답에 현재 요청의 TRACE_ID를 제공한다")
    void returnsCurrentTraceIdForUnexpectedFailure() {
        // given: HTTP나 DB 없이 가짜 TRACE_ID와 예외를 직접 전달한다.
        MDC.put(TraceIdContext.MDC_KEY, "server-trace-1234");
        CustomExceptionHandler handler = new CustomExceptionHandler();

        // when
        ResponseEntity<?> response = handler.handleRuntime(new RuntimeException("database failure"));

        // then
        ResponseDto<?> body = (ResponseDto<?>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getData()).isEqualTo("server-trace-1234");
    }
}
