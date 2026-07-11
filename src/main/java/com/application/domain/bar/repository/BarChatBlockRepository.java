package com.application.domain.bar.repository;

import com.application.domain.bar.entity.BarChatBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BarChatBlockRepository extends JpaRepository<BarChatBlock, BarChatBlock.Key> {

    boolean existsByBlockerRefAndBlockedRef(String blockerRef, String blockedRef);

    @Query("SELECT b.blockedRef FROM BarChatBlock b WHERE b.blockerRef = :blockerRef")
    List<String> findBlockedRefs(@Param("blockerRef") String blockerRef);
}
