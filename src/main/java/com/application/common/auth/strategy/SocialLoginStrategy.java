package com.application.common.auth.strategy;

import com.application.domain.member.entity.ParsedMember;

import java.util.Map;

public interface SocialLoginStrategy {
    Map<String, Object> getUserInfo(String accessToken);
    String getAccessToken(String code, String state);
    String getProviderId(Map<String, Object> userInfo);
    ParsedMember parse(Map<String, Object> userInfo);
}