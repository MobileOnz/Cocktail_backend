package com.application.common.auth.strategy;

import com.application.common.auth.config.OAuth2PropertiesValue;
import com.application.common.exception.custom.CustomApiException;
import com.application.domain.member.entity.ParsedMember;
import com.application.domain.member.enums.Role;
import com.application.domain.member.enums.SocialLogin;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class NaverLoginStartegy implements SocialLoginStrategy{

    private final RestTemplate restTemplate;
    private final OAuth2PropertiesValue oAuth2PropertiesValue;
    private final MemberService memberService;

    @Override
    public Map<String, Object> getUserInfo(String accessToken) {
        String userInfoEndPoint = "https://openapi.naver.com/v1/nid/me";

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

        return (Map<String, Object>)response.getBody().get("response");
    }

    @Override
    public String getAccessToken(String code, String state) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", oAuth2PropertiesValue.getNaverClientId());
        body.add("client_secret", oAuth2PropertiesValue.getNaverClientSecret());
        body.add("redirect_uri", oAuth2PropertiesValue.getNaverRedirectUri());
        body.add("code", code);
        body.add("state", state);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                oAuth2PropertiesValue.getNaverTokenUri(),
                HttpMethod.POST,
                requestEntity,
                Map.class
        );
        return Optional.ofNullable(response.getBody())
                .map(responseBody -> responseBody.get("access_token"))
                .map(Object::toString)
                .orElseThrow(() -> new CustomApiException("Naver AccessToken 값을 가져오지 못했습니다."));
    }

    @Override
    public String getProviderId(Map<String, Object> userInfo) {
        return "naver"+userInfo.get("id").toString();
    }

    @Override
    public ParsedMember parse(Map<String, Object> userInfo) {
        String name = (userInfo.get("name") != null) ? (String) userInfo.get("name") : "N/A";
        String email = (String)userInfo.get("email");

        return ParsedMember.builder()
                .credentialId(getProviderId(userInfo))
                .name(name)
                .email(email)
                .role(Role.USER)
                .socialLogin(SocialLogin.NAVER)
                .build();
    }


}
