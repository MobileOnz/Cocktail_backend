package com.application.domain.monitoring.repository;

import com.application.domain.monitoring.entity.Monitoring;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonitoringRepository extends JpaRepository<Monitoring, Long> {

    Optional<Monitoring> findByDeviceNumber(String deviceNumber);

    boolean existsByDeviceNumber(String deviceNumber);

    void deleteByMemberId(Long memberId);

    List<Monitoring> findAllByMemberId(Long memberId);

    @Query("SELECT COALESCE(SUM(m.count), 0) FROM monitoring m WHERE m.member.id = :memberId")
    Long sumCountByMemberId(@Param("memberId") Long memberId);
}