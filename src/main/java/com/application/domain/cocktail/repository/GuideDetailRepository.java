package com.application.domain.cocktail.repository;

import com.application.domain.cocktail.entity.guide.GuideDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 가이드 세부 단계 Repository.
 *
 * UNIQUE(guide_part, display_order) 제약이 있어 reorder 시 임시 음수 offset 트릭이 필요하므로
 * 벌크 업데이트용 native query 두 개를 함께 제공한다.
 */
public interface GuideDetailRepository extends JpaRepository<GuideDetail, Long> {

    List<GuideDetail> findByGuide_PartOrderByDisplayOrderAsc(Integer guidePart);

    Optional<GuideDetail> findByGuide_PartAndDisplayOrder(Integer guidePart, Integer displayOrder);

    long countByGuide_Part(Integer guidePart);

    /**
     * reorder 1단계: 해당 guide_part 의 모든 detail 의 display_order 를 음수 영역으로 옮긴다.
     * (UNIQUE(guide_part, display_order) 충돌 회피용 임시 값.)
     */
    @Modifying
    @Query(value = "UPDATE guide_detail SET display_order = -display_order - 100000 " +
            "WHERE guide_part = :guidePart", nativeQuery = true)
    int shiftToTemporaryNegative(@Param("guidePart") Integer guidePart);

    /**
     * reorder 2단계: 한 행씩 새 display_order 로 갱신.
     */
    @Modifying
    @Query(value = "UPDATE guide_detail SET display_order = :newOrder " +
            "WHERE id = :id AND guide_part = :guidePart", nativeQuery = true)
    int updateDisplayOrder(@Param("id") Long id,
                           @Param("guidePart") Integer guidePart,
                           @Param("newOrder") Integer newOrder);
}
