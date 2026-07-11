package com.application.domain.bar.repository;

import com.application.domain.bar.entity.BarChatReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BarChatReportRepository extends JpaRepository<BarChatReport, Long> {

    boolean existsByMessageIdAndReporterRef(Long messageId, String reporterRef);

    /** 임계치 판정은 **서로 다른 신고자 수**로 한다. */
    @Query("SELECT COUNT(DISTINCT r.reporterRef) FROM BarChatReport r WHERE r.messageId = :messageId")
    long countDistinctReporters(@Param("messageId") Long messageId);
}
