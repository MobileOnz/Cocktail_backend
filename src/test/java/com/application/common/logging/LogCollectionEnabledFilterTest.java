package com.application.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.core.spi.FilterReply;
import org.junit.jupiter.api.Test;

class LogCollectionEnabledFilterTest {

    @Test
    void allowsFileLoggingByDefault() {
        LogCollectionEnabledFilter filter = new LogCollectionEnabledFilter();

        assertThat(filter.decide(null)).isEqualTo(FilterReply.NEUTRAL);
    }

    @Test
    void deniesFileLoggingWhenDisabled() {
        LogCollectionEnabledFilter filter = new LogCollectionEnabledFilter();
        filter.setEnabled(false);

        assertThat(filter.decide(null)).isEqualTo(FilterReply.DENY);
    }
}
