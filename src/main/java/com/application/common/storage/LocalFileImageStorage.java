package com.application.common.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

/**
 * 로컬 파일시스템 기반 이미지 저장소.
 *
 * Spring 의 ResourceHandler 를 통해 publicBaseUrl 경로로 노출된다 (WebConfig 에서 매핑).
 *
 * 디렉토리 구조:
 *   {localDir}/cocktails/<uuid>_<original>.png
 *   {localDir}/guides/<uuid>_<original>.png
 *
 * 반환 URL 형식:
 *   {publicBaseUrl}/cocktails/<uuid>_<original>.png
 *   ex) http://127.0.0.1/onz/uploads/cocktails/abc123_whisky.png
 */
@Slf4j
public class LocalFileImageStorage implements ImageStorage {

    private final Path localDir;
    private final String publicBaseUrl;

    public LocalFileImageStorage(String localDir, String publicBaseUrl) {
        this.localDir = Paths.get(localDir).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1)
                : publicBaseUrl;
        try {
            Files.createDirectories(this.localDir);
        } catch (IOException e) {
            throw new IllegalStateException("로컬 업로드 디렉토리 생성 실패: " + this.localDir, e);
        }
        log.info("[ImageStorage] LOCAL mode initialized — dir={} publicBaseUrl={}",
                this.localDir, this.publicBaseUrl);
    }

    @Override
    public String upload(String pathPrefix, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("업로드 파일이 비어있습니다.");
        }
        String safePrefix = sanitize(pathPrefix);
        String originalName = sanitize(file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());
        String filename = UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "_" + originalName;

        Path targetDir = localDir.resolve(safePrefix);
        Files.createDirectories(targetDir);

        Path target = targetDir.resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        String url = publicBaseUrl + "/" + safePrefix + "/" + filename;
        log.info("[ImageStorage:LOCAL] uploaded {} bytes → {}", file.getSize(), url);
        return url;
    }

    @Override
    public boolean delete(String url) throws IOException {
        if (url == null || url.isBlank()) return false;
        // publicBaseUrl 로 시작하지 않으면 우리 스토리지 소관 아님 (외부 S3 등) → no-op
        if (!url.startsWith(publicBaseUrl)) {
            log.warn("[ImageStorage:LOCAL] delete skipped — url not in our prefix: {}", url);
            return false;
        }
        String relative = url.substring(publicBaseUrl.length());
        if (relative.startsWith("/")) relative = relative.substring(1);
        Path target = localDir.resolve(relative).normalize();
        // 디렉토리 탈출 방지
        if (!target.startsWith(localDir)) {
            throw new IOException("잘못된 경로: " + url);
        }
        boolean deleted = Files.deleteIfExists(target);
        log.info("[ImageStorage:LOCAL] delete {} → {}", url, deleted ? "removed" : "not found");
        return deleted;
    }

    @Override
    public String mode() {
        return "LOCAL";
    }

    /** path traversal 방지 + 안전한 파일명. */
    private static String sanitize(String s) {
        return s.replaceAll("[^a-zA-Z0-9._\\-]", "_").replaceAll("_+", "_");
    }
}
