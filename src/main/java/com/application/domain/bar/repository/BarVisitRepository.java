package com.application.domain.bar.repository;

import com.application.domain.bar.entity.BarVisit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BarVisitRepository extends JpaRepository<BarVisit, Long> {

    Optional<BarVisit> findByMemberIdAndBarId(Long memberId, Long barId);

    boolean existsByMemberIdAndBarId(Long memberId, Long barId);

    /** 커서 페이징: id DESC. cursor 가 null 이면 첫 페이지. */
    @Query("""
           SELECT v FROM BarVisit v
            WHERE v.memberId = :memberId
              AND (:cursor IS NULL OR v.id < :cursor)
            ORDER BY v.id DESC
           """)
    List<BarVisit> findPageByMember(@Param("memberId") Long memberId,
                                    @Param("cursor") Long cursor,
                                    Pageable pageable);
}
