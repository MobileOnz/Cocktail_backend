package com.application.domain.monitoring.entity;

import com.application.common.time.BaseTimeEntity;
import com.application.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Getter
@Setter
@NoArgsConstructor
@Entity(name="monitoring")
public class Monitoring extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="device_number", nullable = false, unique = true)
    private String deviceNumber;

    @Column(name="count", nullable = false)
    private Long count = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Builder
    public Monitoring(String deviceNumber, Long count, Member member) {
        this.deviceNumber = deviceNumber;
        this.count = count != null ? count : 0L;
        this.member = member;
    }

    /**
     * 접근 횟수 증가 (서버에서 count 관리 시 사용)
     * 현재는 프론트에서 count를 관리하므로 주석처리
     * 향후 서버 관리 방식으로 변경 시 주석 해제
     */
//    public void incrementCount() {
//        this.count++;
//    }

    /**
     * 접근 횟수 업데이트 (프론트에서 전달받은 count 값으로 설정)
     */
    public void updateCount(Long count) {
        this.count = count;
    }

    /**
     * 회원 매핑
     */
    public void mapToMember(Member member) {
        this.member = member;
    }
}