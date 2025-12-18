package com.application.domain.monitoring.service;

import com.application.domain.member.entity.Member;
import com.application.domain.monitoring.dto.ReqTrackingDto;
import com.application.domain.monitoring.dto.ResTrackingDto;
import com.application.domain.monitoring.entity.Monitoring;
import com.application.domain.monitoring.repository.MonitoringRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringService {

    private final MonitoringRepository monitoringRepository;

    /**
     * deviceNumber로 모니터링 레코드 조회 또는 생성
     */
    @Transactional
    public Monitoring createOrGetMonitoring(String deviceNumber) {
        return monitoringRepository.findByDeviceNumber(deviceNumber)
                .orElseGet(() -> {
                    Monitoring newMonitoring = Monitoring.builder()
                            .deviceNumber(deviceNumber)
                            .count(0L)
                            .build();
                    return monitoringRepository.save(newMonitoring);
                });
    }

    /**
     * 접근 횟수 증가 (서버에서 count 관리 시 사용)
     * 현재는 프론트에서 count를 관리하므로 주석처리
     * 향후 서버 관리 방식으로 변경 시 주석 해제
     */
//    @Transactional
//    public void incrementCount(String deviceNumber) {
//        Monitoring monitoring = createOrGetMonitoring(deviceNumber);
//        monitoring.incrementCount();
//        monitoringRepository.save(monitoring);
//        log.info("[MONITORING] Device: {}, Count: {}", deviceNumber, monitoring.getCount());
//    }

    /**
     * 페이지 접근 추적 (프론트에서 전달받은 count 값으로 업데이트)
     */
    @Transactional
    public ResTrackingDto trackPageAccess(ReqTrackingDto reqTrackingDto) {
        String deviceNumber = reqTrackingDto.getDeviceNumber();
        Long count = reqTrackingDto.getCount();

        Optional<Monitoring> existingMonitoring = monitoringRepository.findByDeviceNumber(deviceNumber);

        if (existingMonitoring.isPresent()) {
            // 기존 기기 번호가 있는 경우: count 업데이트
            Monitoring monitoring = existingMonitoring.get();
            monitoring.updateCount(count);
            monitoringRepository.save(monitoring);

            log.info("[MONITORING] Updated - Device: {}, Count: {}", deviceNumber, count);

            return ResTrackingDto.builder()
                    .deviceNumber(deviceNumber)
                    .count(monitoring.getCount())
                    .isFirstAccess(false)
                    .createdAt(monitoring.getCreatedAt())
                    .build();
        } else {
            // 최초 접근: 새로운 레코드 생성
            Monitoring newMonitoring = Monitoring.builder()
                    .deviceNumber(deviceNumber)
                    .count(count)
                    .build();
            Monitoring savedMonitoring = monitoringRepository.save(newMonitoring);

            log.info("[MONITORING] Created - Device: {}, Count: {}, CreatedAt: {}",
                    deviceNumber, count, savedMonitoring.getCreatedAt());

            return ResTrackingDto.builder()
                    .deviceNumber(deviceNumber)
                    .count(savedMonitoring.getCount())
                    .isFirstAccess(true)
                    .createdAt(savedMonitoring.getCreatedAt())
                    .build();
        }
    }

    /**
     * 회원가입 시 모니터링 데이터와 회원 매핑
     */
    @Transactional
    public void mapToMember(String deviceNumber, Member member) {
        if (deviceNumber == null || deviceNumber.isBlank()) {
            return;
        }

        Optional<Monitoring> monitoringOpt = monitoringRepository.findByDeviceNumber(deviceNumber);
        if (monitoringOpt.isPresent()) {
            Monitoring monitoring = monitoringOpt.get();
            monitoring.mapToMember(member);
            monitoringRepository.save(monitoring);
            log.info("[MONITORING] Mapped device {} to member {}", deviceNumber, member.getId());
        } else {
            // 기존 모니터링 데이터가 없으면 새로 생성하여 매핑
            Monitoring newMonitoring = Monitoring.builder()
                    .deviceNumber(deviceNumber)
                    .count(0L)
                    .member(member)
                    .build();
            monitoringRepository.save(newMonitoring);
            log.info("[MONITORING] Created new monitoring for device {} and member {}", deviceNumber, member.getId());
        }
    }

    /**
     * deviceNumber로 모니터링 레코드 조회
     */
    @Transactional(readOnly = true)
    public Optional<Monitoring> getByDeviceNumber(String deviceNumber) {
        return monitoringRepository.findByDeviceNumber(deviceNumber);
    }
}