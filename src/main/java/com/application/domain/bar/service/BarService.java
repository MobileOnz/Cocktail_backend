package com.application.domain.bar.service;

import com.application.common.exception.custom.CustomApiException;
import com.application.common.util.DistanceCalculator;
import com.application.common.util.DistanceCalculator.BoundingBox;
import com.application.domain.bar.dto.response.BarDetailDto;
import com.application.domain.bar.dto.response.BarListItemDto;
import com.application.domain.bar.entity.Bar;
import com.application.domain.bar.repository.BarRepository;
import com.application.domain.bar.repository.BarVisitRepository;
import com.application.domain.bar.repository.SignatureCocktailQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BarService {

    private static final String ACTIVE = "ACTIVE";
    private static final int MAX_LIMIT = 50;
    private static final double MAX_RADIUS_M = 50_000.0;

    private final BarRepository barRepository;
    private final BarVisitRepository barVisitRepository;
    private final SignatureCocktailQuery signatureCocktailQuery;
    private final DistanceCalculator distanceCalculator;

    /**
     * 목록. sort=curated(featuredWeight desc) | recent(updatedAt desc) | distance(lat/lng 필수).
     * 프론트(BarListScreen)는 data 를 **배열 그대로** 받는다.
     */
    public List<BarListItemDto> getBars(String sort, Integer limit, Double lat, Double lng) {
        int size = clampLimit(limit);
        String s = (sort == null || sort.isBlank()) ? "curated" : sort.toLowerCase();

        return switch (s) {
            case "distance" -> {
                if (lat == null || lng == null) {
                    throw new CustomApiException("거리순 정렬에는 lat, lng 이 필요합니다");
                }
                yield nearby(lat, lng, 3_000.0, size);
            }
            case "recent" -> barRepository
                    .findByStatusOrderByUpdatedAtDesc(ACTIVE, PageRequest.of(0, size))
                    .stream().map(b -> BarListItemDto.of(b, null)).toList();
            case "curated" -> barRepository
                    .findByStatusOrderByFeaturedWeightDesc(ACTIVE, PageRequest.of(0, size))
                    .stream().map(b -> BarListItemDto.of(b, null)).toList();
            default -> throw new CustomApiException("지원하지 않는 정렬입니다: " + sort);
        };
    }

    /**
     * 주변 검색. bounding-box 로 DB 선필터(인덱스 사용) → 하버사인으로 정밀 필터 + 정렬.
     * PostGIS 미사용.
     */
    public List<BarListItemDto> nearby(double lat, double lng, Double radiusM, Integer limit) {
        int size = clampLimit(limit);
        double radius = (radiusM == null || radiusM <= 0) ? 3_000.0 : Math.min(radiusM, MAX_RADIUS_M);

        record Scored(Bar bar, double meters) {}

        BoundingBox box = distanceCalculator.boundingBox(lat, lng, radius);
        List<Bar> candidates = barRepository.findWithinBoundingBox(
                ACTIVE,
                BigDecimal.valueOf(box.minLat()), BigDecimal.valueOf(box.maxLat()),
                BigDecimal.valueOf(box.minLng()), BigDecimal.valueOf(box.maxLng()));

        return candidates.stream()
                .map(b -> new Scored(b, distanceCalculator.distanceMeters(
                        lat, lng, b.getLat().doubleValue(), b.getLng().doubleValue())))
                .filter(x -> x.meters() <= radius)                     // 박스 모서리 위양성 제거
                .sorted(Comparator.comparingDouble(Scored::meters))
                .limit(size)
                .map(x -> BarListItemDto.of(x.bar(), round1(x.meters() / 1000.0)))  // FE 는 km 를 기대한다
                .toList();
    }

    /** 상세. memberId 가 null 이면 isVisited=false. */
    public BarDetailDto getBySlug(String slug, Long memberId) {
        Bar bar = barRepository.findBySlugAndStatus(slug, ACTIVE)
                .orElseThrow(() -> new CustomApiException("존재하지 않는 바입니다"));

        boolean visited = memberId != null
                && barVisitRepository.existsByMemberIdAndBarId(memberId, bar.getId());

        return BarDetailDto.of(bar, signatureCocktailQuery.findByBarId(bar.getId()), visited);
    }

    public Bar getActiveBarOrThrow(String slug) {
        return barRepository.findBySlugAndStatus(slug, ACTIVE)
                .orElseThrow(() -> new CustomApiException("존재하지 않는 바입니다"));
    }

    private int clampLimit(Integer limit) {
        if (limit == null || limit <= 0) return 30;
        return Math.min(limit, MAX_LIMIT);
    }

    private double round1(double km) {
        return Math.round(km * 10.0) / 10.0;
    }
}
