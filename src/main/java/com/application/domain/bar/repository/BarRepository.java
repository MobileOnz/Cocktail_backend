package com.application.domain.bar.repository;

import com.application.domain.bar.entity.Bar;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BarRepository extends JpaRepository<Bar, Long> {

    Optional<Bar> findBySlugAndStatus(String slug, String status);

    List<Bar> findByStatusOrderByFeaturedWeightDesc(String status, Pageable pageable);

    List<Bar> findByStatusOrderByUpdatedAtDesc(String status, Pageable pageable);

    /**
     * bounding-box 선필터. lat/lng 복합 인덱스(bar_lat_lng_idx)를 탄다.
     * 원이 아니라 박스이므로 위양성이 섞인다 → 서비스에서 하버사인으로 정밀 필터/정렬한다.
     * PostGIS 미사용.
     */
    @Query("""
           SELECT b FROM Bar b
            WHERE b.status = :status
              AND b.lat BETWEEN :minLat AND :maxLat
              AND b.lng BETWEEN :minLng AND :maxLng
           """)
    List<Bar> findWithinBoundingBox(@Param("status") String status,
                                    @Param("minLat") BigDecimal minLat,
                                    @Param("maxLat") BigDecimal maxLat,
                                    @Param("minLng") BigDecimal minLng,
                                    @Param("maxLng") BigDecimal maxLng);
}
