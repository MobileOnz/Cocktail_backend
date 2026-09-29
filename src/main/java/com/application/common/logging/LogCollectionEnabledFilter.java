package com.application.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/** app.logging.enabled 값에 따라 파일 로그 기록을 허용하거나 차단한다. */
public class LogCollectionEnabledFilter extends Filter<ILoggingEvent> {

    private boolean enabled = true;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public FilterReply decide(ILoggingEvent event) {
        return enabled ? FilterReply.NEUTRAL : FilterReply.DENY;
    }
}
