package com.application.domain.bar.repository;

import com.application.domain.bar.entity.BarVisitSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BarVisitSessionRepository extends JpaRepository<BarVisitSession, Long> {

    Optional<BarVisitSession> findByTokenHash(String tokenHash);

    /** impossible travel 판정용: 이 회원의 가장 최근 세션(다른 바 포함). */
    @Query("""
           SELECT s FROM BarVisitSession s
            WHERE s.memberId = :memberId
              AND s.revoked = false
            ORDER BY s.lastProofAt DESC
           """)
    List<BarVisitSession> findRecentByMember(@Param("memberId") Long memberId, Pageable pageable);

    Optional<BarVisitSession> findByBarIdAndMemberIdAndRevokedFalse(Long barId, Long memberId);

    /** 독립 트랜잭션에서 호출된다(SessionRevoker). 예외로 롤백되면 안 되는 폐기 전용. */
    @Modifying
    @Query("UPDATE BarVisitSession s SET s.revoked = true, s.revokeReason = :reason WHERE s.id = :id")
    int revokeById(@Param("id") Long id, @Param("reason") String reason);
}
