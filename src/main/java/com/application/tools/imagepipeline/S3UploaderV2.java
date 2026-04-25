package com.application.tools.imagepipeline;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Minimal AWS SDK v2 S3 wrapper for the image pipeline. Lazily constructs
 * a single {@link S3Client} only when credentials are available — meaning
 * dry-run mode never instantiates this client and never needs creds.
 */
@Slf4j
@Component
public class S3UploaderV2 {

    private final ImagePipelineProperties props;
    private S3Client client;

    public S3UploaderV2(ImagePipelineProperties props) {
        this.props = props;
    }

    private synchronized S3Client client() {
        if (client == null) {
            if (!props.hasCredentials()) {
                throw new IllegalStateException(
                        "S3UploaderV2.client() called without AWS credentials configured");
            }
            this.client = S3Client.builder()
                    .region(Region.of(props.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())))
                    .build();
            log.info("[S3UploaderV2] initialized region={} bucket={}", props.getRegion(), props.getBucket());
        }
        return client;
    }

    /**
     * Upload bytes to {@code s3://{bucket}/{key}} with the given content-type.
     * Returns the public HTTPS URL using the virtual-hosted-style format.
     */
    public String upload(String key, byte[] body, String contentType) {
        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(props.getBucket())
                .key(key)
                .contentType(contentType)
                .contentLength((long) body.length)
                .acl(ObjectCannedACL.PUBLIC_READ)
                .build();
        client().putObject(req, RequestBody.fromBytes(body));
        return publicUrl(key);
    }

    public String publicUrl(String key) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                props.getBucket(), props.getRegion(), key);
    }
}
