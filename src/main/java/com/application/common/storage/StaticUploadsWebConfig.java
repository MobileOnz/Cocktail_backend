package com.application.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 로컬 업로드 디렉토리를 /uploads/** 경로로 노출하기 위한 ResourceHandler.
 *
 * 컨텍스트 패스 /onz 가 자동 prepend 되므로 실제 외부 URL 은
 *   http://127.0.0.1/onz/uploads/cocktails/<file>
 * 가 된다 (LocalFileImageStorage 가 반환하는 URL 과 일치).
 *
 * S3 모드일 때는 사용하지 않지만 핸들러가 등록돼 있어도 무해 (디렉토리가 비어있으면 404).
 */
@Configuration
public class StaticUploadsWebConfig implements WebMvcConfigurer {

    @Value("${image.upload.local-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolute = Paths.get(uploadDir).toAbsolutePath().normalize().toString();
        // file: 프리픽스 + 끝에 / 필수
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolute + "/")
                .setCachePeriod(60); // 1분 (개발 편의)
    }
}
