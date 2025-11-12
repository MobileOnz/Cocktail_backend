package com.application.common.s3;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

//@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final AmazonS3 amazonS3;

//    @Value("${cloud.aws.s3.bucket}")
    private String bucket;


    public String upload(MultipartFile file, String dir) throws IOException {
        String fileName = dir + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        PutObjectRequest putRequest = new PutObjectRequest(bucket, fileName, file.getInputStream(), metadata);

        amazonS3.putObject(putRequest);

        return amazonS3.getUrl(bucket, fileName).toString();
    }

    public void delete(String url){
        try{
            amazonS3.deleteObject(bucket, extractKeyFromUrl(url));
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }


    private String extractKeyFromUrl(String url) {
        // 도메인 구간 이후의 경로만 잘라내기
        String prefix = String.format("https://%s.s3.%s.amazonaws.com/", bucket, Regions.US_EAST_1);
        if (url.startsWith(prefix)) {
            return url.substring(prefix.length());
        }

        // 만약 regionless (us-east-1 기본 형태) 형식일 경우도 처리
        String defaultPrefix = String.format("https://%s.s3.amazonaws.com/", bucket);
        if (url.startsWith(defaultPrefix)) {
            return url.substring(defaultPrefix.length());
        }

        // 둘 다 안 맞으면 URL 전체에서 마지막 '/' 뒤를 추출 (fallback)
        return url.substring(url.lastIndexOf("/") + 1);
    }





}
