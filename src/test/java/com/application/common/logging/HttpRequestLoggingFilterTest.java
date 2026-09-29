package com.application.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class HttpRequestLoggingFilterTest {

    private final HttpRequestLoggingFilter filter =
            new HttpRequestLoggingFilter(new ActiveRequestRegistry(), 1_000);

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Test
    void generatesTraceIdAndExposesItToRequestLogsAndResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v2/cocktails");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceIdInsideChain = new AtomicReference<>();

        filter.doFilter(
                request,
                response,
                (req, res) -> traceIdInsideChain.set(MDC.get(TraceIdContext.MDC_KEY)));

        String responseTraceId = response.getHeader(TraceIdContext.HEADER_NAME);
        assertThat(responseTraceId).matches("[a-f0-9]{32}");
        assertThat(traceIdInsideChain.get()).isEqualTo(responseTraceId);
        assertThat(MDC.get(TraceIdContext.MDC_KEY)).isNull();
    }

    @Test
    void createsServerTraceIdEvenWhenClientSuppliesOne() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v2/cocktails");
        request.addHeader(TraceIdContext.HEADER_NAME, "mobile-client_1234");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getHeader(TraceIdContext.HEADER_NAME))
                .matches("[a-f0-9]{32}")
                .isNotEqualTo("mobile-client_1234");
    }

    @Test
    void replacesUnsafeClientTraceId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v2/cocktails");
        request.addHeader(TraceIdContext.HEADER_NAME, "bad\nforged-log-line");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getHeader(TraceIdContext.HEADER_NAME))
                .matches("[a-f0-9]{32}")
                .doesNotContain("\n");
    }

    @Test
    void clearsMdcWhenRequestFails() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/failure");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(
                        () ->
                                filter.doFilter(
                                        request,
                                        response,
                                        (req, res) -> {
                                            throw new ServletException("boom");
                                        }))
                .isInstanceOf(ServletException.class);
        assertThat(MDC.get(TraceIdContext.MDC_KEY)).isNull();
        assertThat(response.getHeader(TraceIdContext.HEADER_NAME)).isNotBlank();
    }
}
