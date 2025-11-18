package com.application.common.auth.strategy;

import com.application.common.auth.config.OAuth2PropertiesValue;
import com.application.common.exception.custom.CustomApiException;
import com.application.domain.member.entity.ParsedMember;
import com.application.domain.member.enums.Role;
import com.application.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class KakaoLoginStrategy implements SocialLoginStrategy{
    private final RestTemplate restTemplate;
    private final OAuth2PropertiesValue oAuth2PropertiesValue;
    private final MemberService memberService;

    @Override
    public Map<String, Object> getUserInfo(String accessToken) {

        String userInfoEndPoint = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                userInfoEndPoint,
                HttpMethod.GET,
                entity,
                Map.class
        );


        return response.getBody();
    }

    @Override
    public String getAccessToken(String code, String state) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", oAuth2PropertiesValue.getKakaoClientId());
        body.add("client_secret", oAuth2PropertiesValue.getKakaoClientSecret());
        body.add("redirect_uri", oAuth2PropertiesValue.getKakaoRedirectUri());
        body.add("code", code);

        HttpEntity entity = new HttpEntity(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                oAuth2PropertiesValue.getKakaoTokenUri(),
                HttpMethod.POST,
                entity,
                Map.class
        );

        return Optional.ofNullable(response.getBody())
                .map(responseBody -> responseBody.get("access_token"))
                .map(Object::toString)
                .orElseThrow(() -> new CustomApiException("KAKAO AccessToken 값을 가져오지 못했습니다."));
    }

    @Override
    public String getProviderId(Map<String, Object> userInfo) {
        return "kakao"+userInfo.get("id").toString();
    }

    @Override
    public ParsedMember parse(Map<String, Object> userInfo) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");
        String email = kakaoAccount.get("email").toString();
        Map<String, Object> profile = ((Map<String, Object>) kakaoAccount.get("profile") == null) ? Map.of("nickname",UUID.randomUUID().toString().substring(0,5)) : (Map<String, Object>) kakaoAccount.get("profile");
        String name = profile.get("nickname").toString();

        return ParsedMember.builder()
                .credentialId(getProviderId(userInfo))
                .name(name)
                .email(email)
                .role(Role.USER)
                .build();

    }

}
