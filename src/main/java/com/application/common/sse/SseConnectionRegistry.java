package com.application.common.sse;

import com.application.common.exception.custom.CustomApiException;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SSE 연결 수명 관리 + 스레드 고갈 방어.
 *
 * 왜 스레드가 안전한가:
 *   SseEmitter 는 Servlet 3.1 async 다. 요청 스레드는 emitter 반환 즉시 반납되고,
 *   연결은 커넥터(NIO)가 들고 있는다. 즉 동시 연결 N 개가 톰캣 워커 N 개를 잡지 않는다.
 *   실제로 잡히는 것은 이벤트를 send() 하는 순간뿐이다.
 *
 * 그래도 방어가 필요한 이유:
 *   1. 연결당 힙(emitter + 버퍼)이 쌓인다 → MAX_CONNECTIONS 상한.
 *   2. 죽은 연결(모바일 백그라운드/NAT 타임아웃)은 write 를 해봐야 감지된다 → 15초 ping.
 *   3. emitter timeout 이 없으면 영원히 남는다 → TIMEOUT_MS 후 정상 종료(클라가 재접속).
 */
@Slf4j
@Component
public class SseConnectionRegistry {

    /** 동시 연결 상한. 초과 시 503 을 던져 앱이 폴링으로 강등하게 한다. */
    @Value("${onz.sse.max-connections:200}")
    private int maxConnections;

    /** 이 시간이 지나면 emitter 를 정상 완료시킨다. 클라이언트는 Last-Event-ID 로 재접속한다. */
    @Value("${onz.sse.timeout-ms:1800000}")   // 30분
    private long timeoutMs;

    private final AtomicInteger active = new AtomicInteger(0);
    private final AtomicLong seq = new AtomicLong(0);

    /** emitterKey -> emitter. ping 브로드캐스트 대상. */
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /** emitterKey -> 구독 해제 핸들 */
    private final Map<Long, AutoCloseable> subscriptions = new ConcurrentHashMap<>();

    public long timeoutMs() {
        return timeoutMs;
    }

    /**
     * 연결 슬롯을 확보하고 emitter 를 등록한다.
     * @throws CustomApiException 상한 초과 시
     */
    public long register(SseEmitter emitter, AutoCloseable subscription) {
        if (active.incrementAndGet() > maxConnections) {
            active.decrementAndGet();
            closeQuietly(subscription);
            throw new CustomApiException("실시간 연결이 혼잡합니다. 잠시 후 다시 시도해주세요.");
        }

        long key = seq.incrementAndGet();
        emitters.put(key, emitter);
        subscriptions.put(key, subscription);

        // 세 경우 모두 정확히 한 번 정리되도록 한다(중복 호출은 멱등).
        emitter.onCompletion(() -> release(key));
        emitter.onTimeout(() -> {
            emitter.complete();          // onCompletion 이 이어서 release 한다
        });
        emitter.onError(e -> release(key));

        return key;
    }

    private void release(long key) {
        if (emitters.remove(key) != null) {
            active.decrementAndGet();
        }
        closeQuietly(subscriptions.remove(key));
    }

    private void closeQuietly(AutoCloseable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Exception ignored) {
            // 구독 해제 실패는 무해하다.
        }
    }

    public int activeConnections() {
        return active.get();
    }

    /**
     * 15초 하트비트. Nginx idle timeout 과 모바일 NAT 회수를 막고,
     * **죽은 연결을 감지하는 유일한 수단**이기도 하다(write 실패 → onError → release).
     */
    @Scheduled(fixedRate = 15_000)
    public void heartbeat() {
        emitters.forEach((key, emitter) -> {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException e) {
                // 이미 끊긴 연결. completeWithError 를 부르면 onError → release 로 이어진다.
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) {
                    release(key);
                }
            }
        });
    }

    @PreDestroy
    public void shutdown() {
        emitters.forEach((key, emitter) -> {
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // 종료 중이므로 무시
            }
        });
        emitters.clear();
        subscriptions.values().forEach(this::closeQuietly);
        subscriptions.clear();
        active.set(0);
    }
}
