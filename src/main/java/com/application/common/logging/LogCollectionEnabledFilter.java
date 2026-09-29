package com.application.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * 설정을 껐을 때 파일 수집만 중단하고 기동 장애 확인용 콘솔 로그는 유지하는 Logback 필터다.
 * Logback이 {@link #setEnabled(boolean)}로 app.logging.enabled 값을 주입한다.
 */
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
