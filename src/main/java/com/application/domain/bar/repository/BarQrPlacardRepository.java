package com.application.domain.bar.repository;

import com.application.domain.bar.entity.BarQrPlacard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BarQrPlacardRepository extends JpaRepository<BarQrPlacard, Long> {

    Optional<BarQrPlacard> findByBarIdAndKeyVersion(Long barId, Integer keyVersion);
}
