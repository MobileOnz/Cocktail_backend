package com.application.web.services.auth.factory;

import com.application.common.exception.custom.CustomApiException;
import com.application.web.services.auth.strategy.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class SocialLoginFactory {
    private final NaverLoginStartegy naverLoginStartegy;
    private final GoogleLoginStartegy googleLoginStartegy;
    private final KakaoLoginStrategy kakaoLoginStategy;
    private final AppleLoginStrategy appleLoginStrategy;

    public SocialLoginStrategy getLoginStrategy(String provider){
        switch (provider.toLowerCase()){
            case "naver":
                return naverLoginStartegy;
            case "google":
                return googleLoginStartegy;
            case "kakao":
                return kakaoLoginStategy;
            case "apple":
                return appleLoginStrategy;
            default:
                throw new CustomApiException("No Support " + provider);
        }
    }
}
