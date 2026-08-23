package com.application.domain.bar.repository;

import com.application.domain.bar.entity.BarChatIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface BarChatIdentityRepository extends JpaRepository<BarChatIdentity, String> {

    @Modifying
    @Query("DELETE FROM BarChatIdentity i WHERE i.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
