package com.application.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("HTTP 요청 로깅 필터")
class HttpRequestLoggingFilterTest {

    private final HttpRequestLoggingFilter filter =
            new HttpRequestLoggingFilter(new ActiveRequestRegistry(), 1_000);

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    @DisplayName("서버 TRACE_ID를 생성해 요청 로그와 응답 헤더에 동일하게 제공한다")
    void generatesTraceIdAndExposesItToRequestLogsAndResponse() throws Exception {
        // given: 실제 서버와 DB 없이 Spring Mock 요청/응답만 사용한다.
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v2/cocktails");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceIdInsideChain = new AtomicReference<>();

        // when
        filter.doFilter(
                request,
                response,
                (req, res) -> traceIdInsideChain.set(MDC.get(TraceIdContext.MDC_KEY)));

        // then
        String responseTraceId = response.getHeader(TraceIdContext.HEADER_NAME);
        assertThat(responseTraceId).matches("[a-f0-9]{32}");
        assertThat(traceIdInsideChain.get()).isEqualTo(responseTraceId);
        assertThat(MDC.get(TraceIdContext.MDC_KEY)).isNull();
    }

    @Test
    @DisplayName("클라이언트가 TRACE_ID를 보내도 서버가 안전한 ID로 교체한다")
    void createsServerTraceIdEvenWhenClientSuppliesOne() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v2/cocktails");
        request.addHeader(TraceIdContext.HEADER_NAME, "mobile-client_1234");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, (req, res) -> {});

        // then
        assertThat(response.getHeader(TraceIdContext.HEADER_NAME))
                .matches("[a-f0-9]{32}")
                .isNotEqualTo("mobile-client_1234");
    }

    @Test
    @DisplayName("개행이 포함된 외부 TRACE_ID로 로그 라인을 위조할 수 없다")
    void replacesUnsafeClientTraceId() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v2/cocktails");
        request.addHeader(TraceIdContext.HEADER_NAME, "bad\nforged-log-line");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, (req, res) -> {});

        // then
        assertThat(response.getHeader(TraceIdContext.HEADER_NAME))
                .matches("[a-f0-9]{32}")
                .doesNotContain("\n");
    }

    @Test
    @DisplayName("요청 처리 중 예외가 발생해도 MDC를 정리하고 TRACE_ID를 응답한다")
    void clearsMdcWhenRequestFails() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/failure");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when & then
        assertThatThrownBy(
                        () ->
                                filter.doFilter(
                                        request,
                                        response,
                                        (req, res) -> {
                                            throw new ServletException("boom");
                                        }))
                .isInstanceOf(ServletException.class);

        // then
        assertThat(MDC.get(TraceIdContext.MDC_KEY)).isNull();
        assertThat(response.getHeader(TraceIdContext.HEADER_NAME)).isNotBlank();
    }
}
