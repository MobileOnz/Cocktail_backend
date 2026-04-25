package com.application.common.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

/**
 * S3 기반 이미지 저장소. AWS SDK v2 사용.
 *
 * AWS_S3_ACCESS_KEY / AWS_S3_SECRET_KEY / S3_BUCKET 가 주입돼야 동작.
 * 자격증명이 없으면 ImageStorageConfig 에서 LocalFileImageStorage 가 대신 활성화됨.
 *
 * S3 키 패턴: {pathPrefix}/{uuid}_{original-filename}
 * 반환 URL : https://{bucket}.s3.{region}.amazonaws.com/{key}
 */
@Slf4j
public class S3ImageStorage implements ImageStorage {

    private final S3Client client;
    private final String bucket;
    private final String region;

    public S3ImageStorage(String accessKey, String secretKey, String region, String bucket) {
        this.bucket = bucket;
        this.region = region;
        this.client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
        log.info("[ImageStorage] S3 mode initialized — bucket={} region={}", bucket, region);
    }

    @Override
    public String upload(String pathPrefix, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("업로드 파일이 비어있습니다.");
        }
        String safePrefix = sanitize(pathPrefix);
        String safeName = sanitize(file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());
        String key = safePrefix + "/" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "_" + safeName;

        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        try {
            client.putObject(req, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (S3Exception e) {
            log.error("[ImageStorage:S3] put failed bucket={} key={} : {}", bucket, key, e.awsErrorDetails().errorMessage(), e);
            throw new IOException("S3 업로드 실패: " + e.awsErrorDetails().errorMessage(), e);
        }

        String url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
        log.info("[ImageStorage:S3] uploaded {} bytes → {}", file.getSize(), url);
        return url;
    }

    @Override
    public boolean delete(String url) throws IOException {
        if (url == null || url.isBlank()) return false;
        // URL → key 추출
        String marker = ".amazonaws.com/";
        int idx = url.indexOf(marker);
        if (idx < 0) {
            log.warn("[ImageStorage:S3] delete skipped — url not in S3 form: {}", url);
            return false;
        }
        String key = url.substring(idx + marker.length());
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
            log.info("[ImageStorage:S3] deleted s3://{}/{}", bucket, key);
            return true;
        } catch (S3Exception e) {
            throw new IOException("S3 삭제 실패: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public String mode() {
        return "S3";
    }

    private static String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9._\\-]", "_").replaceAll("_+", "_");
    }
}
