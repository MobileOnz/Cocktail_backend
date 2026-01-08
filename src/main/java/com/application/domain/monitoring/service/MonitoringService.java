package com.application.domain.monitoring.service;

import com.application.common.exception.custom.CustomApiException;
import com.application.domain.member.entity.Member;
import com.application.domain.member.enums.AgeRange;
import com.application.domain.member.enums.Gender;
import com.application.domain.monitoring.dto.response.SaveOnboardingReq;
import com.application.domain.monitoring.dto.response.TrackingReq;
import com.application.domain.monitoring.dto.request.MonitoringInfoRes;
import com.application.domain.monitoring.dto.response.OnboardingStatusRes;
import com.application.domain.monitoring.dto.request.TrackingRes;
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
     * 회원이면 전체 count 합산 반환, 비회원이면 기기별 count 반환
     */
    @Transactional
    public TrackingRes trackPageAccess(TrackingReq reqTrackingDto) {
        String deviceNumber = reqTrackingDto.getDeviceNumber();
        Long count = reqTrackingDto.getCount();

        Optional<Monitoring> existingMonitoring = monitoringRepository.findByDeviceNumber(deviceNumber);

        if (existingMonitoring.isPresent()) {
            // 기존 기기 번호가 있는 경우: count 업데이트
            Monitoring monitoring = existingMonitoring.get();
            monitoring.updateCount(count);
            monitoringRepository.save(monitoring);

            Member member = monitoring.getMember();
            boolean isMember = (member != null);
            Long totalCount = isMember ? getTotalCountByMember(member) : monitoring.getCount();

            log.info("[MONITORING] Updated - Device: {}, Count: {}, IsMember: {}, TotalCount: {}",
                    deviceNumber, count, isMember, totalCount);

            return TrackingRes.builder()
                    .deviceNumber(deviceNumber)
                    .count(totalCount)
                    .isFirstAccess(false)
                    .createdAt(monitoring.getCreatedAt())
                    .isMember(isMember)
                    .memberId(isMember ? member.getId() : null)
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

            return TrackingRes.builder()
                    .deviceNumber(deviceNumber)
                    .count(savedMonitoring.getCount())
                    .isFirstAccess(true)
                    .createdAt(savedMonitoring.getCreatedAt())
                    .isMember(false)
                    .memberId(null)
                    .build();
        }
    }

    /**
     * 회원가입/로그인 시 모니터링 데이터와 회원 매핑
     * 비회원 상태에서 온보딩을 완료한 경우, 온보딩 데이터를 Member로 복사
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

            // 비회원 온보딩 데이터가 있으면 Member로 복사
            if (monitoring.getOnboardingCompleted() && monitoring.getGender() != null && monitoring.getAgeRange() != null) {
                // Member에 온보딩 정보가 없을 때만 복사
                if (member.getGender() == null || member.getAgeRange() == null) {
                    member.setGender(monitoring.getGender());
                    member.setAgeRange(monitoring.getAgeRange());
                    log.info("[ONBOARDING] Merged non-member onboarding to member - Device: {}, MemberId: {}, Gender: {}, AgeRange: {}",
                            deviceNumber, member.getId(), monitoring.getGender(), monitoring.getAgeRange());

                    // 해당 회원의 모든 기기의 온보딩 상태를 완료로 동기화
                    monitoringRepository.findAllByMemberId(member.getId()).forEach(m -> {
                        m.markOnboardingCompleted();
                        monitoringRepository.save(m);
                    });
                } else {
                    // Member에 이미 온보딩 정보가 있으면, Monitoring의 온보딩 완료 상태만 동기화
                    monitoring.markOnboardingCompleted();
                }
            }

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

    /**
     * 회원의 모든 기기 count 합산 조회
     */
    @Transactional(readOnly = true)
    public Long getTotalCountByMember(Member member) {
        if (member == null) {
            return 0L;
        }
        return monitoringRepository.sumCountByMemberId(member.getId());
    }

    /**
     * 회원 ID로 모든 기기 count 합산 조회
     */
    @Transactional(readOnly = true)
    public Long getTotalCountByMemberId(Long memberId) {
        if (memberId == null) {
            return 0L;
        }
        return monitoringRepository.sumCountByMemberId(memberId);
    }

    /**
     * deviceNumber로 기기 정보, 연령, 성별 조회
     * 회원이면 회원 정보 포함, 비회원이면 기기 정보만 반환
     */
    @Transactional(readOnly = true)
    public MonitoringInfoRes getMonitoringInfo(String deviceNumber) {
        Monitoring monitoring = monitoringRepository.findByDeviceNumber(deviceNumber)
                .orElseThrow(() -> new CustomApiException("해당 기기 정보를 찾을 수 없습니다."));

        Member member = monitoring.getMember();
        boolean isMember = (member != null);
        Long totalCount = isMember ? getTotalCountByMember(member) : monitoring.getCount();

        return MonitoringInfoRes.builder()
                .deviceNumber(deviceNumber)
                .isMember(isMember)
                .memberId(isMember ? member.getId() : null)
                .age(isMember ? member.getAge() : null)
                .ageRange(isMember ? member.getAgeRange() : null)
                .gender(isMember ? member.getGender() : null)
                .totalCount(totalCount)
                .build();
    }

    /**
     * deviceNumber로 온보딩 상태 확인
     * 회원이면 Member의 온보딩 정보 확인, 비회원이면 Monitoring의 온보딩 정보 확인
     */
    @Transactional(readOnly = true)
    public OnboardingStatusRes getOnboardingStatus(String deviceNumber) {
        Optional<Monitoring> monitoringOpt = monitoringRepository.findByDeviceNumber(deviceNumber);

        if (monitoringOpt.isEmpty()) {
            // 최초 접근: 온보딩 필요
            log.info("[ONBOARDING] First access for device: {}", deviceNumber);
            return OnboardingStatusRes.builder()
                    .deviceNumber(deviceNumber)
                    .onboardingCompleted(false)
                    .requiresOnboarding(true)
                    .isMember(false)
                    .build();
        }

        Monitoring monitoring = monitoringOpt.get();
        Member member = monitoring.getMember();
        boolean isMember = (member != null);
        boolean onboardingCompleted;

        if (isMember) {
            // 회원: Member의 gender와 ageRange 확인
            onboardingCompleted = (member.getGender() != null && member.getAgeRange() != null);
            log.info("[ONBOARDING] Member check - Device: {}, MemberId: {}, Completed: {}",
                    deviceNumber, member.getId(), onboardingCompleted);
        } else {
            // 비회원: Monitoring의 onboardingCompleted 확인
            onboardingCompleted = monitoring.getOnboardingCompleted();
            log.info("[ONBOARDING] Non-member check - Device: {}, Completed: {}",
                    deviceNumber, onboardingCompleted);
        }

        return OnboardingStatusRes.builder()
                .deviceNumber(deviceNumber)
                .onboardingCompleted(onboardingCompleted)
                .requiresOnboarding(!onboardingCompleted)
                .isMember(isMember)
                .build();
    }

    /**
     * 비회원 온보딩 정보 저장
     * Monitoring에 gender, ageRange, onboardingCompleted를 저장
     */
    @Transactional
    public void saveNonMemberOnboarding(SaveOnboardingReq dto) {
        String deviceNumber = dto.getDeviceNumber();

        Gender gender = Gender.fromString(dto.getGender())
                .orElseThrow(() -> new CustomApiException("Invalid Gender Type"));
        AgeRange ageRange = AgeRange.fromString(dto.getAgeRange())
                .orElseThrow(() -> new CustomApiException("Invalid Age Range Type"));

        Monitoring monitoring = monitoringRepository.findByDeviceNumber(deviceNumber)
                .orElseGet(() -> {
                    // 기기 정보가 없으면 새로 생성
                    Monitoring newMonitoring = Monitoring.builder()
                            .deviceNumber(deviceNumber)
                            .count(0L)
                            .build();
                    return monitoringRepository.save(newMonitoring);
                });

        // 이미 회원인 경우 에러
        if (monitoring.getMember() != null) {
            throw new CustomApiException("이미 회원으로 등록된 기기입니다. 회원 온보딩 API를 사용하세요.");
        }

        // 온보딩 정보 저장
        monitoring.saveOnboardingInfo(gender, ageRange);
        monitoringRepository.save(monitoring);

        log.info("[ONBOARDING] Non-member onboarding saved - Device: {}, Gender: {}, AgeRange: {}",
                deviceNumber, gender, ageRange);
    }
}