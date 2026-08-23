package com.application.domain.bar.support;

import com.application.common.auth.dto.oauth2Dto.CustomOAuth2User;
import com.application.common.exception.custom.CustomApiException;
import com.application.domain.member.entity.Member;
import com.application.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * principal → member.id 해석.
 *
 * ⚠️ 현재 JWTFilter 는 화이트리스트(fail-open) 방식이고, 바 경로는 아직 등록되어 있지 않다
 *    (JWTFilter/SecurityConfig 는 s3 소유라 이 태스크에서 건드리지 않는다).
 *    그래서 /visit, /me/visits 는 지금 principal 이 항상 null 로 들어온다.
 *    → resolveIdOrThrow() 가 401 을 던져 "조용히 아무것도 안 하는" F-08 류 사고를 원천 차단한다.
 *    s3 의 deny-by-default 반전(T-03) 이후에는 필터가 먼저 401 을 반환하므로 이 경로는 도달하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class BarMemberResolver {

    private final MemberRepository memberRepository;

    /** 비로그인 허용(OPTIONAL 인증) 경로용. */
    public Long resolveIdOrNull(CustomOAuth2User principal) {
        if (principal == null || principal.getCredentialId() == null) return null;
        Member member = memberRepository.findByCredentialId(principal.getCredentialId());
        return member == null ? null : member.getId();
    }

    /** 로그인 필수(JWT) 경로용. */
    public Long resolveIdOrThrow(CustomOAuth2User principal) {
        Long id = resolveIdOrNull(principal);
        if (id == null) {
            throw new CustomApiException("로그인이 필요한 서비스입니다.");
        }
        return id;
    }
}
