package com.application.domain.monitoring.repository;

import com.application.domain.monitoring.entity.Monitoring;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MonitoringRepository extends JpaRepository<Monitoring, Long> {

    Optional<Monitoring> findByDeviceNumber(String deviceNumber);

    boolean existsByDeviceNumber(String deviceNumber);
}