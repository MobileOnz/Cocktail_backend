package com.application.common.storage;

import com.application.tools.imagepipeline.ImagePipelineProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ImageStorage 빈 팩토리.
 *
 * AWS S3 자격증명(AWS_S3_ACCESS_KEY / SECRET_KEY) 가 모두 채워져 있으면 → S3 모드
 * 비어 있거나 부족하면 → 로컬 파일 모드 (개발/테스트 환경)
 *
 * 운영 EC2 에선 환경변수만 주입하면 자동으로 S3 모드가 활성화된다.
 */
@Slf4j
@Configuration
public class ImageStorageConfig {

    @Bean
    public ImageStorage imageStorage(
            ImagePipelineProperties s3Props,
            @Value("${image.upload.local-dir:./uploads}") String localDir,
            @Value("${image.upload.public-base-url:http://127.0.0.1/onz/uploads}") String publicBaseUrl
    ) {
        boolean hasAwsCreds = s3Props.getAccessKey() != null && !s3Props.getAccessKey().isBlank()
                && s3Props.getSecretKey() != null && !s3Props.getSecretKey().isBlank();

        if (hasAwsCreds) {
            log.info("[ImageStorageConfig] AWS creds detected → using S3 storage");
            return new S3ImageStorage(
                    s3Props.getAccessKey(),
                    s3Props.getSecretKey(),
                    s3Props.getRegion(),
                    s3Props.getBucket()
            );
        }
        log.info("[ImageStorageConfig] no AWS creds → using LOCAL file storage (dir={})", localDir);
        return new LocalFileImageStorage(localDir, publicBaseUrl);
    }
}
