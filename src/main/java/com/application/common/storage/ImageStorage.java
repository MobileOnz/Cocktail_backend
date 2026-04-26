package com.application.common.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 이미지 스토리지 추상화.
 *
 * 로컬 개발 환경에선 파일시스템에 저장하고, 운영 환경에선 S3 PutObject로 저장.
 * 어떤 구현이 활성화될지는 ImageStorageConfig 의 @Bean 팩토리가
 * aws.s3.access-key 가 비어있는지 여부로 결정한다.
 */
public interface ImageStorage {

    /**
     * @param pathPrefix  e.g. "cocktails", "guides"
     * @param file        업로드된 파일
     * @return 공개 URL (저장 후 즉시 GET 으로 접근 가능해야 함)
     */
    String upload(String pathPrefix, MultipartFile file) throws IOException;

    /**
     * @param url upload() 이 반환했던 URL. 로컬 모드면 파일 삭제, S3 모드면 DeleteObject.
     * @return 삭제 성공 여부 (대상이 이미 없으면 false 반환, 예외는 throw)
     */
    boolean delete(String url) throws IOException;

    /** 로깅/관리용 모드 식별자 (LOCAL or S3) */
    String mode();
}
