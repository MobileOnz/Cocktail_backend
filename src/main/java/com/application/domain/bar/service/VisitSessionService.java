package com.application.domain.bar.service;

import com.application.common.exception.custom.CustomApiException;
import com.application.domain.bar.exception.BarAccessException;
import com.application.common.ratelimit.RateLimiter;
import com.application.common.util.DistanceCalculator;
import com.application.domain.bar.dto.request.VisitSessionRequest;
import com.application.domain.bar.dto.response.VisitSessionDto;
import com.application.domain.bar.entity.Bar;
import com.application.domain.bar.entity.BarVisitSession;
import com.application.domain.bar.repository.BarVisitSessionRepository;
import com.application.domain.bar.trust.TrustLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/**
 * 방문 세션 발급/갱신 — 신뢰등급 판정의 유일한 진입점.
 *
 * 판정 매트릭스:
 *   반경 밖                                   → 403 OUT_OF_RANGE   (세션 없음 = L0)
 *   반경 안, QR 없음                          → L1  (채팅만. ★ 구버전 앱 하위호환 ★)
 *   반경 안, QR 유효, mock=false              → L2  (채팅 + 가격)
 *   반경 안, QR 유효, mock=true               → L1  (강등)
 *   반경 안, QR 위조                          → 403 INVALID_QR     (거부. 조용히 L1 주지 않는다)
 *   물리적으로 불가능한 이동                   → 403 IMPOSSIBLE_TRAVEL + 기존 세션 폐기
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitSessionService {

    /** GPS 반경. accuracy 를 절반만 보정해준다(과도한 관대함 방지). */
    @Value("${onz.bar.geofence-m:150}")
    private double geofenceM;

    /** 세션 수명. 30분마다 renew 로 근접을 재증명해야 한다. */
    @Value("${onz.bar.session-ttl-minutes:30}")
    private long ttlMinutes;

    /** 이 속도를 넘는 이동은 물리적으로 불가능하다고 본다(km/h). */
    @Value("${onz.bar.max-travel-kmh:300}")
    private double maxTravelKmh;

    /** 정확도가 이보다 나쁘면 L2 를 주지 않는다(실내 GPS 는 보통 10~50m). */
    @Value("${onz.bar.l2-max-accuracy-m:100}")
    private double l2MaxAccuracyM;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final BarService barService;
    private final BarVisitSessionRepository sessionRepository;
    private final QrSignatureService qrSignatureService;
    private final AnonHandleService anonHandleService;
    private final DistanceCalculator distanceCalculator;
    private final RateLimiter rateLimiter;
    private final SessionRevoker sessionRevoker;

    // ── 발급 ───────────────────────────────────────────────────────────

    @Transactional
    public VisitSessionDto issue(String slug, Long memberId, VisitSessionRequest req) {
        Bar bar = barService.getActiveBarOrThrow(slug);
        requireCoordinates(req);

        if (!rateLimiter.tryAcquire("qr:member:" + memberId, 10, Duration.ofHours(1))) {
            throw BarAccessException.tooManyAttempts();
        }

        double distanceM = distanceTo(bar, req);
        assertWithinGeofence(distanceM);
        assertPossibleTravel(memberId, req, distanceM);

        TrustLevel trust = decideTrustLevel(slug, bar.getId(), req);

        // 같은 바에 이미 세션이 있으면 재사용하지 않고 폐기 후 새로 낸다.
        // (등급이 바뀔 수 있고, 토큰 회전이 항상 안전한 쪽이다.)
        sessionRepository.findByBarIdAndMemberIdAndRevokedFalse(bar.getId(), memberId)
                .ifPresent(old -> old.revoke("REISSUED"));

        String rawToken = newToken();
        BarVisitSession session = sessionRepository.save(BarVisitSession.issue(
                bar.getId(), memberId, sha256(rawToken), trust,
                LocalDateTime.now().plusMinutes(ttlMinutes),
                BigDecimal.valueOf(req.lat()), BigDecimal.valueOf(req.lng()),
                Boolean.TRUE.equals(req.mockLocationDetected())));

        var identity = anonHandleService.resolve(memberId, bar.getId());
        return VisitSessionDto.of(session, rawToken, distanceM, identity.getNickname(), identity.getAuthorRef());
    }

    // ── 갱신 ───────────────────────────────────────────────────────────

    @Transactional
    public VisitSessionDto renew(String slug, String sessionToken, VisitSessionRequest req) {
        Bar bar = barService.getActiveBarOrThrow(slug);
        requireCoordinates(req);

        BarVisitSession session = requireActiveSession(bar.getId(), sessionToken);

        double distanceM = distanceTo(bar, req);
        assertWithinGeofence(distanceM);

        // 갱신 시에도 등급을 다시 판정한다. QR 을 다시 주지 않으면 L1 로 내려간다 —
        // "한 번 L2 면 3시간 내내 L2" 를 허용하지 않기 위해서다.
        TrustLevel trust = decideTrustLevel(slug, bar.getId(), req);
        session.renew(trust, LocalDateTime.now().plusMinutes(ttlMinutes),
                BigDecimal.valueOf(req.lat()), BigDecimal.valueOf(req.lng()),
                Boolean.TRUE.equals(req.mockLocationDetected()));

        var identity = anonHandleService.resolve(session.getMemberId(), bar.getId());
        return VisitSessionDto.of(session, sessionToken, distanceM,
                identity.getNickname(), identity.getAuthorRef());
    }

    // ── 조회 (TrustLevelResolver / 채팅이 사용) ──────────────────────────

    @Transactional(readOnly = true)
    public BarVisitSession requireActiveSession(Long barId, String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw BarAccessException.sessionRequired();
        }
        BarVisitSession session = sessionRepository.findByTokenHash(sha256(sessionToken))
                .orElseThrow(BarAccessException::sessionInvalid);

        if (!session.getBarId().equals(barId)) {
            // 다른 매장의 세션. 존재 사실 자체를 알리지 않는다.
            throw BarAccessException.sessionInvalid();
        }
        if (!session.isActive(LocalDateTime.now())) {
            throw BarAccessException.sessionExpired();
        }
        return session;
    }

    /** 판정 불가 시 예외 대신 null. TrustLevelResolver 가 L0 으로 번역한다. */
    @Transactional(readOnly = true)
    public BarVisitSession findActiveSessionOrNull(Long barId, String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) return null;
        return sessionRepository.findByTokenHash(sha256(sessionToken))
                .filter(s -> s.getBarId().equals(barId))
                .filter(s -> s.isActive(LocalDateTime.now()))
                .orElse(null);
    }

    // ── 판정 로직 ───────────────────────────────────────────────────────

    private TrustLevel decideTrustLevel(String slug, Long barId, VisitSessionRequest req) {
        boolean hasQr = req.qrPayload() != null && !req.qrPayload().isBlank();

        if (!hasQr) {
            // QR 없이 GPS 만. 현재 앱스토어 배포본이 이 경로로 채팅에 들어간다. 깨면 안 된다.
            return TrustLevel.L1;
        }

        if (!qrSignatureService.verify(slug, barId, req.qrPayload())) {
            // 위조 QR 을 조용히 L1 로 통과시키지 않는다. 명시적으로 거부한다.
            throw BarAccessException.invalidQr();
        }

        if (Boolean.TRUE.equals(req.mockLocationDetected())) {
            // 탐지 사실을 사용자에게 알리지 않는다. 조용히 강등한다.
            log.warn("mock location 의심 → L2 강등 거부: slug={}", slug);
            return TrustLevel.L1;
        }

        if (req.accuracyM() != null && req.accuracyM() > l2MaxAccuracyM) {
            return TrustLevel.L1;   // 정확도가 나쁘면 가격을 열지 않는다
        }

        return TrustLevel.L2;
    }

    private void assertWithinGeofence(double distanceM) {
        if (distanceM > geofenceM) {
            throw BarAccessException.outOfRange(geofenceM, distanceM);
        }
    }

    /**
     * impossible travel. 직전 세션의 좌표/시각과 비교해 물리적으로 불가능한 이동이면
     * 기존 세션을 폐기하고 거부한다. 계정 공유·토큰 유출의 가장 저렴한 탐지 수단이다.
     */
    private void assertPossibleTravel(Long memberId, VisitSessionRequest req, double distanceToBarM) {
        List<BarVisitSession> recent = sessionRepository.findRecentByMember(memberId, PageRequest.of(0, 1));
        if (recent.isEmpty()) return;

        BarVisitSession last = recent.get(0);
        double movedM = distanceCalculator.distanceMeters(
                last.getLastLat().doubleValue(), last.getLastLng().doubleValue(), req.lat(), req.lng());

        long elapsedSec = Duration.between(last.getLastProofAt(), LocalDateTime.now()).getSeconds();
        if (elapsedSec <= 0) elapsedSec = 1;

        // 같은 자리에서의 재발급은 언제나 정상이다.
        if (movedM < 200) return;

        double kmh = (movedM / 1000.0) / (elapsedSec / 3600.0);
        if (kmh > maxTravelKmh) {
            // 별도 트랜잭션으로 커밋한다. 아래 throw 로 현재 트랜잭션이 롤백되어도 폐기는 남는다.
            sessionRevoker.revoke(last.getId(), "IMPOSSIBLE_TRAVEL");
            log.warn("impossible travel: memberId={} {}m in {}s = {}km/h", memberId, Math.round(movedM), elapsedSec, Math.round(kmh));
            throw BarAccessException.impossibleTravel();
        }
    }

    private double distanceTo(Bar bar, VisitSessionRequest req) {
        return distanceCalculator.distanceMeters(
                bar.getLat().doubleValue(), bar.getLng().doubleValue(), req.lat(), req.lng());
    }

    private void requireCoordinates(VisitSessionRequest req) {
        if (req == null || req.lat() == null || req.lng() == null) {
            throw new CustomApiException("위치 정보가 필요합니다");
        }
    }

    // ── 토큰 ───────────────────────────────────────────────────────────

    private String newToken() {
        byte[] buf = new byte[32];   // 256bit
        RANDOM.nextBytes(buf);
        return "vs_" + Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    /** DB 에는 해시만 저장한다. 유출되어도 원문 토큰을 복원할 수 없다. */
    private String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("토큰 해시 실패", e);
        }
    }
}
