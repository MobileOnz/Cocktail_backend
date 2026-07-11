package com.application.domain.cocktail.service;

import com.application.domain.cocktail.dto.response.CocktailGuideDto;
import com.application.domain.cocktail.dto.response.CocktailStepDto;
import com.application.domain.cocktail.entity.CocktailStep;
import com.application.domain.cocktail.repository.CocktailStepRepository;
import com.application.domain.member.entity.Member;
import com.application.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * T-07 서비스. 기존 CocktailService(다른 세션 소유)를 건드리지 않기 위해 별도 클래스로 분리.
 */
@Service
@RequiredArgsConstructor
public class CocktailStepService {

    private final CocktailStepRepository stepRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<CocktailStepDto> getSteps(Long cocktailId) {
        List<Long> toolIds = stepRepository.findToolIdsByCocktailId(cocktailId);
        return stepRepository.findByCocktailIdOrderByStepOrderAsc(cocktailId).stream()
                .map((CocktailStep s) -> CocktailStepDto.from(s, toolIds))
                .toList();
    }

    /** '이 칵테일의 이야기' — 연결된 가이드 목록. */
    @Transactional(readOnly = true)
    public List<CocktailGuideDto> getGuides(Long cocktailId) {
        return stepRepository.findGuidesByCocktailId(cocktailId).stream()
                .map(CocktailGuideDto::from)
                .toList();
    }

    public boolean cocktailExists(Long cocktailId) {
        return stepRepository.cocktailExists(cocktailId);
    }

    /**
     * "만들어봤어요" 기록. credentialId 로 회원을 찾아 멱등 저장한다.
     * @return true=처리됨, false=회원/칵테일 없음
     */
    @Transactional
    public boolean recordMade(String credentialId, Long cocktailId) {
        if (credentialId == null) return false;
        if (!stepRepository.cocktailExists(cocktailId)) return false;
        Member member = memberRepository.findByCredentialId(credentialId);
        if (member == null) return false;
        stepRepository.recordMade(member.getId(), cocktailId);
        return true;
    }
}
