package com.application.domain.bar.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 매장 접근 거부. reason 코드가 앱의 분기 기준이다(문구가 아니라).
 *
 * 공유 파일(CustomExceptionHandler)을 건드리지 않기 위해 bar 도메인 전용 advice 로 처리한다.
 */
@Getter
public class BarAccessException extends RuntimeException {

    private final HttpStatus status;
    private final String reason;
    private final Object detail;

    public BarAccessException(HttpStatus status, String reason, String message, Object detail) {
        super(message);
        this.status = status;
        this.reason = reason;
        this.detail = detail;
    }

    public static BarAccessException outOfRange(double geofenceM, double distanceM) {
        return new BarAccessException(HttpStatus.FORBIDDEN, "OUT_OF_RANGE",
                String.format("매장에서 %.0fm 이내에서만 인증할 수 있어요 (현재 %.0fm)", geofenceM, distanceM),
                new Detail(Math.round(distanceM * 10.0) / 10.0));
    }

    public static BarAccessException invalidQr() {
        return new BarAccessException(HttpStatus.FORBIDDEN, "INVALID_QR",
                "ONZ 매장 QR이 아니거나 만료된 QR이에요", null);
    }

    /** 탐지 사실을 사용자에게 알리지 않는다. */
    public static BarAccessException impossibleTravel() {
        return new BarAccessException(HttpStatus.FORBIDDEN, "IMPOSSIBLE_TRAVEL",
                "위치 정보를 확인할 수 없어요", null);
    }

    public static BarAccessException tooManyAttempts() {
        return new BarAccessException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS",
                "잠시 후 다시 시도해주세요", null);
    }

    public static BarAccessException sessionRequired() {
        return new BarAccessException(HttpStatus.UNAUTHORIZED, "SESSION_REQUIRED",
                "매장 인증이 필요합니다. 매장 근처에서 다시 시도해주세요.", null);
    }

    public static BarAccessException sessionExpired() {
        return new BarAccessException(HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED",
                "매장 인증이 만료됐어요. 다시 인증해주세요.", null);
    }

    public static BarAccessException sessionInvalid() {
        return new BarAccessException(HttpStatus.UNAUTHORIZED, "SESSION_INVALID",
                "매장 세션이 유효하지 않습니다.", null);
    }

    public static BarAccessException rateLimited(String message) {
        return new BarAccessException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", message, null);
    }

    public static BarAccessException muted() {
        return new BarAccessException(HttpStatus.FORBIDDEN, "MUTED",
                "신고 누적으로 발언이 제한된 상태입니다.", null);
    }

    public record Detail(Double distanceM) {}
}
