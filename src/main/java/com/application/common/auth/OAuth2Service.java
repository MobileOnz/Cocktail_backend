package com.application.common.auth;

import com.application.common.auth.dto.login.*;
import com.application.common.auth.factory.SocialLoginFactory;
import com.application.common.auth.jwt.JWTUtil;
import com.application.common.auth.strategy.SocialLoginStrategy;
import com.application.common.cache.CacheType;
import com.application.common.exception.custom.CustomApiException;
import com.application.common.exception.custom.TokenInvalidException;
import com.application.domain.member.entity.Member;
import com.application.domain.member.entity.ParsedMember;
import com.application.domain.member.enums.Role;
import com.application.domain.member.service.MemberService;
import com.application.domain.monitoring.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class OAuth2Service {

    private final JWTUtil jwtUtil;
    private final JWTStoreService jwtStoreService;
    private final JWTAccessTokenBlackListService jwtAccessTokenBlackListService;

    private final SocialLoginFactory factory;
    private final MemberService memberService;
    private final MonitoringService monitoringService;
    private final CacheManager cacheManager;


    public ResSocialLoginDto socialLogin(ReqSocialLoginDto reqSocialLoginDto) {
        SocialLoginStrategy strategy = (SocialLoginStrategy) factory.getLoginStrategy(reqSocialLoginDto.getProvider());
        Map<String, Object> userInfo = extractUserInfo(reqSocialLoginDto, strategy);

        if (userInfo == null || userInfo.isEmpty()) {
            throw new CustomApiException("Fail Social Login");
        }

        ParsedMember parsedMember = strategy.parse(userInfo);
        return handleMemberLoginFlow(parsedMember, reqSocialLoginDto.getDeviceNumber());
    }

    /**
     * 회원가입 : 프론트엔드에서 약관 동의 후, /signup api 호출 시, 사용자 정보 DB 저장
     */
    public ResSocialLoginDto signup(ReqSignupDto reqSignupDto){
        Cache cache =  cacheManager.getCache(CacheType.PARSED_MEMBER.getName());
        if(cache == null){
            throw new CustomApiException("소셜 로그인 정보를 찾을 수 없습니다.");
        }

        ParsedMember parsedMember = cache.get(reqSignupDto.getCode(), ParsedMember.class);
        if (parsedMember == null ){
            throw new CustomApiException("만료되었거나, 잘못된 코드값입니다.");
        }

        Member newMember = Member.builder()
                .name(parsedMember.getName())
                .nickname(reqSignupDto.getNickName())
                .credentialId(parsedMember.getCredentialId())
                .email(parsedMember.getEmail())
                .role(parsedMember.getRole())
                .socialLogin(parsedMember.getSocialLogin())
                .ageTerm(reqSignupDto.getAgeTerm())
                .serviceTerm(reqSignupDto.getServiceTerm())
                .marketingTerm(reqSignupDto.getMarketingTerm())
                .adTerm(reqSignupDto.getAdTerm())
                .build();

        memberService.saveMember(newMember);

        // 모니터링 데이터와 회원 매핑
        monitoringService.mapToMember(reqSignupDto.getDeviceNumber(), newMember);

        cache.evict(reqSignupDto.getCode());

        return createJWTToken(newMember);
    }

    public ResTokenDto reissueRefreshToken(String refreshToken){
        checkRefreshToken(refreshToken);
        return updateRefreshToken(refreshToken);
    }


    public void logout(String fullAccessToken){
        String accessToken = fullAccessToken.substring(7);
        if (jwtStoreService.containKey(jwtUtil.getUUID(accessToken))){
            jwtStoreService.deleteByKey(jwtUtil.getUUID(accessToken));
            jwtAccessTokenBlackListService.addBlackList(jwtUtil.getUUID(accessToken), accessToken);
        }else{
            jwtAccessTokenBlackListService.addBlackList(jwtUtil.getUUID(accessToken), accessToken);
        }
    }


    private Map<String, Object> extractUserInfo(ReqSocialLoginDto reqSocialLoginDto, SocialLoginStrategy strategy) {
        Map<String, Object> userInfo;

        // 토큰 값 자체는 남기지 않는다. 로그를 읽을 수 있는 사람이 그대로 계정을 쓸 수 있다.
        log.info("[SOCIAL_LOGIN] provider: {}, accessTokenProvided: {}",
                reqSocialLoginDto.getProvider(),
                reqSocialLoginDto.getAccessToken() != null && !reqSocialLoginDto.getAccessToken().isBlank());

        if(reqSocialLoginDto.getProvider().equalsIgnoreCase("apple")){
            if(reqSocialLoginDto.getAccessToken() == null || reqSocialLoginDto.getAccessToken().isBlank()){
                userInfo = strategy.getUserInfo(strategy.getAccessToken(reqSocialLoginDto.getCode(), reqSocialLoginDto.getState()));
            }else{
                userInfo = strategy.getUserInfo(reqSocialLoginDto.getAccessToken());
            }
        } else if (reqSocialLoginDto.getAccessToken() == null || reqSocialLoginDto.getAccessToken().isBlank()) {
            log.info("[INFO] GET CODE VALUE LOGIC");
            userInfo = strategy.getUserInfo(strategy.getAccessToken(reqSocialLoginDto.getCode(), reqSocialLoginDto.getState()));
        }else {
            log.info("[INFO] GET ACCESS TOKEN VALUE LOGIC");
            userInfo = strategy.getUserInfo(reqSocialLoginDto.getAccessToken());
        }

        return userInfo;
    }


    /**
     * 로그인을 시도한 회원이 기존 회원인지 확인
     */
    private ResSocialLoginDto handleMemberLoginFlow(ParsedMember parsedMember, String deviceNumber) {
        Member member = memberService.getMemberByCredentialId(parsedMember.getCredentialId());
        if(member == null){
            // [신규 회원] DB에 없으면 -> '회원가입 대기 상태'
            return createCode(parsedMember);
        }else{
            // [기존 회원] DB에 있으면 -> 기기-회원 매핑 후 로그인 성공 (JWT 토큰 발급)
            if (deviceNumber != null && !deviceNumber.isBlank()) {
                monitoringService.mapToMember(deviceNumber, member);
                log.info("[LOGIN] Device {} mapped to member {}", deviceNumber, member.getId());
            }
            return createJWTToken(member);
        }
    }

    /**
     * 신규 회원 시, 임시 저장
     * - 캐시에 정보를 잠깐 넣어두고, 임시 코드만 프론트에 전달
     */
    private ResSignupDto createCode(ParsedMember parsedMember){
        String code = UUID.randomUUID().toString(); // 임시 코드 생성

        // 캐시 메모리에 저장
        Cache cache = cacheManager.getCache(CacheType.PARSED_MEMBER.getName());
        cache.put(code, parsedMember);

        // 프론트에 신규 회원임을 알림 : 사용자 임시코드 생성 및 전달 -> 사용자 약관 >> sign-up
        return ResSignupDto.builder()
                .code(code)
                .build();
    }

    private ResTokenDto createJWTToken(Member member){

        String uuid = UUID.randomUUID().toString();
        String accessToken = jwtUtil.createAccessJwt(uuid, member.getCredentialId(),Role.USER.getEnglish());
        String refreshToken = jwtUtil.createRefreshJwt(uuid, member.getCredentialId(), Role.USER.getEnglish());

        jwtStoreService.save(jwtUtil.getUUID(refreshToken), refreshToken);

        log.info("JWT 토큰 발급 완료. uuid: {}", jwtUtil.getUUID(refreshToken));

        return new ResTokenDto(accessToken, refreshToken);
    }


    private void checkRefreshToken(String refreshToken){
        String uuid = jwtUtil.getUUID(refreshToken);
        if(jwtUtil.isRefreshExpired(refreshToken) || !jwtStoreService.containKey(uuid)){
            throw new TokenInvalidException("Refresh Token Expired");
        }else{
            jwtStoreService.deleteByKey(uuid);
        }
    }

    private ResTokenDto updateRefreshToken(String refreshToken){
        String uuid = UUID.randomUUID().toString();
        String newAccessToken = jwtUtil.createAccessJwt(uuid, jwtUtil.getCredentialId(refreshToken),jwtUtil.getRole(refreshToken) );
        String newRefreshToken = jwtUtil.createRefreshJwt(uuid, jwtUtil.getCredentialId(refreshToken),jwtUtil.getRole(refreshToken) );
        log.info("리프레시 토큰 재발급 완료. uuid: {}", uuid);

        jwtStoreService.save(uuid, newRefreshToken);

        return new ResTokenDto(newAccessToken, newRefreshToken);
    }

}
