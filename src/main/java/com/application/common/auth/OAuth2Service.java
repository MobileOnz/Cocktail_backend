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
    private final CacheManager cacheManager;


    public ResSocialLoginDto socialLogin(ReqSocialLoginDto reqSocialLoginDto) {
        SocialLoginStrategy strategy = (SocialLoginStrategy) factory.getLoginStrategy(reqSocialLoginDto.getProvider());
        Map<String, Object> userInfo = extractUserInfo(reqSocialLoginDto, strategy);

        if (userInfo == null || userInfo.isEmpty()) {
            throw new CustomApiException("Fail Social Login");
        }

        ParsedMember parsedMember = strategy.parse(userInfo);
        return handleMemberLoginFlow(parsedMember);
    }

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

        log.info("[SOCITAL_LOGIN] : {}", reqSocialLoginDto.getAccessToken());

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


    private ResSocialLoginDto handleMemberLoginFlow(ParsedMember parsedMember) {
        Member member = memberService.getMemberByCredentialId(parsedMember.getCredentialId());
        if(member == null){
            return createCode(parsedMember);
        }else{
            return createJWTToken(member);
        }
    }

    private ResSignupDto createCode(ParsedMember parsedMember){
        String code = UUID.randomUUID().toString();

        Cache cache = cacheManager.getCache(CacheType.PARSED_MEMBER.getName());
        cache.put(code, parsedMember);

        return ResSignupDto.builder()
                .code(code)
                .build();
    }

    private ResTokenDto createJWTToken(Member member){

        String uuid = UUID.randomUUID().toString();
        String accessToken = jwtUtil.createAccessJwt(uuid, member.getCredentialId(),Role.USER.getEnglish());
        String refreshToken = jwtUtil.createRefreshJwt(uuid, member.getCredentialId(), Role.USER.getEnglish());

        jwtStoreService.save(jwtUtil.getUUID(refreshToken), refreshToken);

        log.info("uuid: " + jwtUtil.getUUID(refreshToken));
        log.info("jwtToken : " + accessToken);
        log.info("refreshToken : " + jwtStoreService.findByKey(jwtUtil.getUUID(refreshToken)).getRefreshToken());

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
        log.info("new Refresh Key : " + newRefreshToken);

        jwtStoreService.save(uuid, newRefreshToken);

        return new ResTokenDto(newAccessToken, newRefreshToken);
    }

}
