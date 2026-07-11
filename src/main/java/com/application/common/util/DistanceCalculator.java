package com.application.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 하버사인 거리 계산 + bounding-box 산출.
 *
 * PostGIS 를 도입하지 않는다(plan_FINAL D-15): 바가 수백 개 규모이고, 단일 EC2 Postgres 에
 * extension 을 더하는 것은 순수 리스크다. 대신 lat/lng 복합 인덱스로 bounding-box 선필터를
 * DB 에서 수행하고, 정밀 거리 계산과 정렬은 애플리케이션에서 한다.
 *
 * 기존 isWithinRadius(...) 는 참조 0건의 데드코드였다(감사 F-21). 시그니처를 보존한 채 부활시킨다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DistanceCalculator {

    private static final double EARTH_RADIUS_M = 6_371_000.0;

    /** 위도 1도당 미터. 경도는 위도가 높아질수록 좁아지므로 cos 보정이 필요하다. */
    private static final double METERS_PER_DEG_LAT = 111_320.0;

    /** 두 좌표 사이 거리(미터). */
    public double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }

    /** 기존 시그니처 보존 (킬로미터 단위 반경 판정). */
    public boolean isWithinRadius(double lat1, double lon1, double lat2, double lon2, double radiusKm) {
        return distanceMeters(lat1, lon1, lat2, lon2) <= radiusKm * 1000.0;
    }

    /**
     * 반경을 감싸는 경계 상자. DB 선필터(WHERE lat BETWEEN .. AND lng BETWEEN ..)에 쓴다.
     * 상자는 원을 포함하므로 모서리에 위양성이 섞인다 → 이후 하버사인으로 다시 거른다.
     */
    public BoundingBox boundingBox(double lat, double lng, double radiusMeters) {
        double dLat = radiusMeters / METERS_PER_DEG_LAT;

        // 극점 근처에서 cos → 0 이 되어 dLng 가 발산하는 것을 막는다.
        double cosLat = Math.cos(Math.toRadians(lat));
        double dLng = Math.abs(cosLat) < 1e-6
                ? 180.0
                : radiusMeters / (METERS_PER_DEG_LAT * cosLat);

        return new BoundingBox(
                clampLat(lat - dLat), clampLat(lat + dLat),
                lng - Math.abs(dLng), lng + Math.abs(dLng)
        );
    }

    private double clampLat(double v) {
        return Math.max(-90.0, Math.min(90.0, v));
    }

    /** 선필터용 경계 상자. */
    public record BoundingBox(double minLat, double maxLat, double minLng, double maxLng) {}
}
