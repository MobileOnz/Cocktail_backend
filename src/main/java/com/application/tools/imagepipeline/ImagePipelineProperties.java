package com.application.tools.imagepipeline;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Bound to the {@code aws.s3.*} block in {@code application.yml}.
 *
 * <p>Used by the one-shot image pipeline runner. Note that this is intentionally
 * <strong>separate</strong> from the legacy {@link com.application.common.config.S3Config}
 * (Spring Cloud AWS v1) — the image pipeline talks to S3 directly via AWS SDK v2.
 *
 * <p>If either {@link #accessKey} or {@link #secretKey} is blank, the runner forces
 * dry-run mode (download only, write variants to local disk).
 */
@Configuration
@ConfigurationProperties(prefix = "aws.s3")
public class ImagePipelineProperties {

    /** Region that hosts the bucket. Defaults to ap-northeast-2 (Seoul) in yml. */
    private String region;

    /** Bucket name (default: onz-cocktail-images). */
    private String bucket;

    /** AWS access key. Blank → dry-run forced. */
    private String accessKey;

    /** AWS secret key. Blank → dry-run forced. */
    private String secretKey;

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }

    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    /** Returns true iff both access-key and secret-key are non-blank. */
    public boolean hasCredentials() {
        return accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }
}
