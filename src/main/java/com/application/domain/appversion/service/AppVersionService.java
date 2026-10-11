package com.application.domain.appversion.service;

import com.application.domain.appversion.dto.AppVersionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/**
 * 플랫폼별 최신·최소 버전. 값은 환경변수로 바꾼다(재시작만 하면 되고 마이그레이션·앱 재심사가 없다).
 *
 * <p>출시 절차: 스토어 승인 후 LATEST 를 올린다. 옛 버전을 막아야 할 때만 MIN 을 올린다.
 */
@Service
public class AppVersionService {

    private final AppVersionResponse ios;
    private final AppVersionResponse android;

    public AppVersionService(
            @Value("${onz.app-version.ios.latest}") String iosLatest,
            @Value("${onz.app-version.ios.min}") String iosMin,
            @Value("${onz.app-version.ios.store-url}") String iosStoreUrl,
            @Value("${onz.app-version.android.latest}") String androidLatest,
            @Value("${onz.app-version.android.min}") String androidMin,
            @Value("${onz.app-version.android.store-url}") String androidStoreUrl) {
        this.ios = new AppVersionResponse("ios", iosLatest, iosMin, iosStoreUrl);
        this.android = new AppVersionResponse("android", androidLatest, androidMin, androidStoreUrl);
    }

    public AppVersionResponse get(String platform) {
        return switch (platform == null ? "" : platform.toLowerCase(Locale.ROOT)) {
            case "ios" -> ios;
            case "android" -> android;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "platform 은 ios 또는 android 여야 합니다.");
        };
    }
}
