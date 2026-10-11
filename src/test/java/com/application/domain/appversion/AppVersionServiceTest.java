package com.application.domain.appversion;

import com.application.domain.appversion.dto.AppVersionResponse;
import com.application.domain.appversion.service.AppVersionService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppVersionServiceTest {

    private final AppVersionService service = new AppVersionService(
            "2.1.0", "2.0.0", "https://apps.apple.com/x",
            "2.1.1", "0.0.0", "https://play.google.com/x");

    @Test
    void 플랫폼별_값을_돌려준다() {
        assertThat(service.get("ios"))
                .isEqualTo(new AppVersionResponse("ios", "2.1.0", "2.0.0", "https://apps.apple.com/x"));
        assertThat(service.get("android").latestVersion()).isEqualTo("2.1.1");
    }

    @Test
    void 대소문자를_가리지_않는다() {
        assertThat(service.get("iOS").platform()).isEqualTo("ios");
    }

    @Test
    void 모르는_플랫폼은_400() {
        assertThatThrownBy(() -> service.get("windows")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.get(null)).isInstanceOf(ResponseStatusException.class);
    }
}
