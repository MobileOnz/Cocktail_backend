package com.application.domain.inquiry.service;

import com.application.common.auth.dto.oauth2Dto.CustomOAuth2User;
import com.application.common.exception.custom.CustomApiException;
import com.application.common.ratelimit.RateLimiter;
import com.application.domain.inquiry.dto.InquiryCreateRequestDto;
import com.application.domain.inquiry.entity.Inquiry;
import com.application.domain.inquiry.repository.InquiryRepository;
import com.application.domain.member.entity.Member;
import com.application.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * 1:1 문의 접수.
 *
 * <p>앱에 문의 화면과 '문의 보내기' 버튼은 있었는데 받는 API 가 없어서 전송이 항상 실패했다.
 * 어드민의 문의 목록이 시드 3건뿐이었던 이유도 이것이다.</p>
 *
 * <p>로그인하지 않아도 보낼 수 있어야 한다(로그인이 안 돼서 문의하는 경우가 실제로 있다).
 * 그래서 인증은 선택이고, 대신 남용을 막는 책임이 여기로 온다 — 아래 레이트리밋 참고.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiries;
    private final MemberRepository members;
    private final RateLimiter rateLimiter;

    /**
     * 문의를 받는다.
     *
     * @param user 로그인 사용자(없으면 null)
     * @param clientKey 레이트리밋 기준 키. 로그인 사용자는 회원 식별자, 비로그인은 기기ID 또는 IP.
     */
    @Transactional
    public Long create(InquiryCreateRequestDto req, CustomOAuth2User user, String clientKey) {
        // 무인증으로 열려 있는 쓰기 경로다. 사람이 손으로 쓰는 속도엔 걸리지 않고
        // 스크립트로 쏟아붓는 것만 막는 수준으로 잡는다.
        if (!rateLimiter.tryAcquire("inquiry:burst:" + clientKey, 3, Duration.ofMinutes(1))
                || !rateLimiter.tryAcquire("inquiry:day:" + clientKey, 20, Duration.ofHours(24))) {
            // 전역 핸들러가 CustomApiException 의 메시지를 그대로 400 으로 내려준다.
            // 앱은 res.data.msg 를 그대로 토스트에 띄우므로 문구가 곧 사용자 안내다.
            throw new CustomApiException("문의가 너무 잦습니다. 잠시 후 다시 시도해 주세요.");
        }

        Long memberId = resolveMemberId(user);

        // 비로그인인데 연락처도 기기ID도 없으면 회신할 방법도, 같은 사람인지 알 방법도 없다.
        if (memberId == null
                && isBlank(req.contact())
                && isBlank(req.deviceNumber())) {
            throw new CustomApiException("연락받을 수 있는 정보를 입력해 주세요.");
        }

        Inquiry saved = inquiries.save(Inquiry.builder()
                .memberId(memberId)
                .deviceNumber(trimToNull(req.deviceNumber()))
                .email(trimToNull(req.contact()))
                .title(req.title().trim())
                .content(req.content().trim())
                .status("NEW")
                .build());

        log.info("[INQUIRY] 접수 id={} memberId={} 연락처유무={}",
                saved.getId(), memberId, !isBlank(req.contact()));
        return saved.getId();
    }

    /**
     * 토큰이 있어도 회원을 못 찾는 경우가 있다(탈퇴 직후 등). 그럴 땐 익명 문의로 받는다 —
     * 문의를 못 넣게 막는 것보다 받아두는 쪽이 낫다.
     */
    private Long resolveMemberId(CustomOAuth2User user) {
        if (user == null || user.getCredentialId() == null) {
            return null;
        }
        Member member = members.findByCredentialId(user.getCredentialId());
        if (member == null) {
            log.warn("[INQUIRY] 토큰은 있으나 회원 없음 — 익명으로 접수한다");
            return null;
        }
        return member.getId();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trimToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }
}
