package com.application.domain.bar.repository;

import com.application.domain.bar.entity.BarChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BarChatMessageRepository extends JpaRepository<BarChatMessage, Long> {

    /** SSE Last-Event-ID 재개 백필. id > lastId 인 VISIBLE 메시지를 오래된 순으로. */
    @Query("""
           SELECT m FROM BarChatMessage m
            WHERE m.barId = :barId AND m.id > :lastId AND m.status = 'VISIBLE'
            ORDER BY m.id ASC
           """)
    List<BarChatMessage> findAfterId(@Param("barId") Long barId, @Param("lastId") Long lastId, Pageable pageable);

    /** 최초 접속 시 최근 N건(오래된 순으로 뒤집어 반환하기 위해 desc 로 뽑는다). */
    @Query("""
           SELECT m FROM BarChatMessage m
            WHERE m.barId = :barId AND m.status = 'VISIBLE'
            ORDER BY m.id DESC
           """)
    List<BarChatMessage> findRecent(@Param("barId") Long barId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM BarChatMessage m WHERE m.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
