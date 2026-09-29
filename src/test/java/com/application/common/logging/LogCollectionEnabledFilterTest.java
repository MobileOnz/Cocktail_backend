package com.application.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("파일 로그 수집 ON/OFF 필터")
class LogCollectionEnabledFilterTest {

    @Test
    @DisplayName("기본 설정에서는 파일 로그 기록을 허용한다")
    void allowsFileLoggingByDefault() {
        // given
        LogCollectionEnabledFilter filter = new LogCollectionEnabledFilter();

        // when
        FilterReply result = filter.decide(null);

        // then
        assertThat(result).isEqualTo(FilterReply.NEUTRAL);
    }

    @Test
    @DisplayName("수집 설정이 OFF이면 파일 로그 기록을 거부한다")
    void deniesFileLoggingWhenDisabled() {
        // given
        LogCollectionEnabledFilter filter = new LogCollectionEnabledFilter();
        filter.setEnabled(false);

        // when
        FilterReply result = filter.decide(null);

        // then
        assertThat(result).isEqualTo(FilterReply.DENY);
    }
}
