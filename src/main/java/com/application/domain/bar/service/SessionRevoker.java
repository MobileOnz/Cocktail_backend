package com.application.domain.bar.service;

import com.application.domain.bar.repository.BarVisitSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 세션 폐기를 **독립 트랜잭션**에서 커밋한다.
 *
 * 왜 별도 빈인가: impossible travel 을 탐지하면 (1) 기존 세션을 폐기하고 (2) 요청을 거부해야 하는데,
 * 같은 트랜잭션 안에서 예외를 던지면 (1)까지 롤백되어 **폐기가 사라진다.**
 * 자기호출(self-invocation)은 프록시를 타지 않으므로 별도 빈으로 분리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionRevoker {

    private final BarVisitSessionRepository sessionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void revoke(Long sessionId, String reason) {
        int updated = sessionRepository.revokeById(sessionId, reason);
        log.warn("세션 폐기: id={} reason={} updated={}", sessionId, reason, updated);
    }
}
