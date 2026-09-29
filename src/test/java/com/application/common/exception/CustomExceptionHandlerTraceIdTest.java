package com.application.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.application.common.logging.TraceIdContext;
import com.application.common.response.ResponseDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;

class CustomExceptionHandlerTraceIdTest {

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void returnsCurrentTraceIdForUnexpectedFailure() {
        MDC.put(TraceIdContext.MDC_KEY, "server-trace-1234");
        CustomExceptionHandler handler = new CustomExceptionHandler();

        ResponseEntity<?> response = handler.handleRuntime(new RuntimeException("database failure"));

        ResponseDto<?> body = (ResponseDto<?>) response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getData()).isEqualTo("server-trace-1234");
    }
}
