package com.application.web.services.auth.strategy;

import com.application.common.auth.config.OAuth2PropertiesValue;
import com.application.common.exception.custom.CustomApiException;
import com.application.domain.member.entity.Member;
import com.application.domain.member.entity.ParsedMember;
import com.application.domain.member.enums.Role;
import com.application.web.services.member.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleLoginStartegy implements SocialLoginStrategy{
    private final RestTemplate restTemplate;
    private final OAuth2PropertiesValue oAuth2PropertiesValue;
    private final MemberService memberService;

    @Override
    public Map<String, Object> getUserInfo(String accessToken) {
        String userInfoEndPoint = "https://people.googleapis.com/v1/people/me?personFields=emailAddresses,names";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try{
            ResponseEntity<Map> response = restTemplate.exchange(
                    userInfoEndPoint,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            return response.getBody();
        }catch(Exception e){
            log.error("[Exception] Google API 호출 오류 : {}", e.getMessage());
            throw new CustomApiException("Google API 호출 실패 "+ e.getMessage());
        }
    }

    @Override
    public String getAccessToken(String code, String state) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", oAuth2PropertiesValue.getGoogleClientId());
        body.add("client_secret", oAuth2PropertiesValue.getGoogleClientSecret());
        body.add("redirect_uri", oAuth2PropertiesValue.getGoogleRedirectUri());
        body.add("code", code);

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body,headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                oAuth2PropertiesValue.getGoogleTokenUri(),
                HttpMethod.POST,
                requestEntity,
                Map.class
        );

        return Optional.ofNullable(response.getBody())
                .map(responseBody -> responseBody.get("access_token"))
                .map(Object::toString)
                .orElseThrow(() -> new CustomApiException("구글에서 토큰을 가져오지 못했습니다."));
    }

    @Override
    public String getProviderId(Map<String, Object> userInfo) {
        return Optional.ofNullable(userInfo.get("resourceName"))
                .map(Object::toString)
                .map(value -> "google" + value.split("/")[1])
                .orElseThrow(() -> new CustomApiException("Provider ID를 찾을 수 없습니다."));
    }

    @Override
    public ParsedMember parse(Map<String, Object> userInfo) {
        List<Map<String, Object>> namesList = (List<Map<String, Object>>) userInfo.get("names");
        String displayName = namesList.get(0).get("displayName").toString();
        List<Map<String, Object>> emailList = (List<Map<String, Object>>) userInfo.get("emailAddresses");
        String email = emailList.get(0).get("value").toString();

        return ParsedMember.builder()
                .credentialId(getProviderId(userInfo))
                .name(displayName)
                .email(email)
                .role(Role.USER)
                .build();
    }


}
