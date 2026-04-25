package com.application.tools.imagepipeline;

import com.application.domain.cocktail.entity.Cocktail;
import com.application.domain.cocktail.repository.CocktailRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * One-shot CLI runner that converts every {@link Cocktail#getImageUrl()} and
 * {@link Cocktail#getGlassImageUrl()} PNG into 2 WebP variants (thumb / detail).
 *
 * <h3>Activation</h3>
 * Only runs when {@code --image-pipeline} is on the command line. Otherwise the
 * Spring web server starts normally and this runner is a no-op.
 *
 * <h3>Modes</h3>
 * <ul>
 *   <li><strong>dry-run</strong> — explicit {@code --dry-run} OR missing AWS creds.
 *       Saves variants to {@code /tmp/onz-image-out/{cocktails|glasses}/...} and
 *       skips the DB update.</li>
 *   <li><strong>full</strong> — uploads to S3 and updates DB columns
 *       (image_url_thumb / image_url_detail / glass_image_url_thumb /
 *       glass_image_url_detail).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImagePipelineRunner implements ApplicationRunner {

    private static final String FLAG_PIPELINE = "image-pipeline";
    private static final String FLAG_DRY_RUN = "dry-run";

    private static final Path OUT_ROOT = Path.of("/tmp/onz-image-out");
    private static final String S3_PREFIX_COCKTAILS = "cocktails";
    private static final String S3_PREFIX_GLASSES = "glasses";

    private final CocktailRepository cocktailRepository;
    private final ImageVariantGenerator generator;
    private final S3UploaderV2 uploader;
    private final ImagePipelineProperties props;
    private final EntityManager em;
    private final ConfigurableApplicationContext appContext;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!args.containsOption(FLAG_PIPELINE) && !args.getNonOptionArgs().contains("--" + FLAG_PIPELINE)) {
            // Normal Spring Boot startup — do nothing.
            return;
        }

        boolean explicitDryRun = args.containsOption(FLAG_DRY_RUN)
                || args.getNonOptionArgs().contains("--" + FLAG_DRY_RUN);
        boolean credsMissing = !props.hasCredentials();
        boolean dryRun = explicitDryRun || credsMissing;

        log.info("================================================================");
        log.info("  ONZ Image Pipeline Runner");
        log.info("  flag --image-pipeline = present");
        log.info("  flag --dry-run        = {}", explicitDryRun);
        log.info("  AWS creds present     = {}", !credsMissing);
        log.info("  effective mode        = {}", dryRun ? "DRY-RUN (no S3 upload, no DB update)" : "FULL (S3 + DB)");
        if (credsMissing && !explicitDryRun) {
            log.warn("  >> AWS credentials missing — forcing dry-run mode.");
        }
        log.info("  region={} bucket={}", props.getRegion(), props.getBucket());
        log.info("================================================================");

        if (dryRun) {
            Files.createDirectories(OUT_ROOT.resolve(S3_PREFIX_COCKTAILS));
            Files.createDirectories(OUT_ROOT.resolve(S3_PREFIX_GLASSES));
        }

        List<Cocktail> all = cocktailRepository.findAll();
        log.info("Loaded {} cocktails", all.size());

        Stats stats = new Stats();
        for (Cocktail c : all) {
            processOne(c, dryRun, stats);
        }

        log.info("================================================================");
        log.info("  Image pipeline complete.");
        log.info("  cocktails seen           : {}", stats.seen);
        log.info("  image_url processed      : {}", stats.cocktailProcessed);
        log.info("  image_url skipped (null) : {}", stats.cocktailSkipped);
        log.info("  image_url failed         : {}", stats.cocktailFailed);
        log.info("  glass_url processed      : {}", stats.glassProcessed);
        log.info("  glass_url skipped (null) : {}", stats.glassSkipped);
        log.info("  glass_url failed         : {}", stats.glassFailed);
        log.info("  total bytes downloaded   : {} ({} KB)", stats.bytesIn, stats.bytesIn / 1024);
        log.info("  total bytes generated    : {} ({} KB)", stats.bytesOut, stats.bytesOut / 1024);
        log.info("  bytes saved (in - out)   : {} ({} KB, {}% reduction)",
                stats.bytesIn - stats.bytesOut,
                (stats.bytesIn - stats.bytesOut) / 1024,
                stats.bytesIn == 0 ? 0 : (100L * (stats.bytesIn - stats.bytesOut) / stats.bytesIn));
        if (dryRun) {
            log.info("  output dir               : {}", OUT_ROOT.toAbsolutePath());
        }
        log.info("================================================================");

        // One-shot tool: exit cleanly so the JVM doesn't linger as a web server.
        // We close the context (instead of System.exit) so Spring runs all
        // shutdown hooks (DB pool, etc.) gracefully.
        log.info("Image pipeline finished. Closing Spring context and exiting.");
        new Thread(() -> {
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            int code = stats.cocktailFailed + stats.glassFailed > 0 ? 1 : 0;
            appContext.close();
            System.exit(code);
        }, "image-pipeline-shutdown").start();
    }

    private void processOne(Cocktail c, boolean dryRun, Stats stats) {
        stats.seen++;
        String label = "[id=" + c.getId() + " " + c.getKorName() + "]";

        String imgUrl = nullIfBlank(c.getImageUrl());
        if (imgUrl == null) {
            stats.cocktailSkipped++;
            log.info("{} image_url is null — skipping cocktail variant", label);
        } else {
            try {
                processVariant(label + " cocktail", imgUrl, c.getId(), S3_PREFIX_COCKTAILS, dryRun, stats, true);
                stats.cocktailProcessed++;
            } catch (Exception e) {
                stats.cocktailFailed++;
                log.error("{} cocktail FAILED: {}", label, e.toString());
            }
        }

        String glassUrl = nullIfBlank(c.getGlassImageUrl());
        if (glassUrl == null) {
            stats.glassSkipped++;
            log.info("{} glass_image_url is null — skipping glass variant", label);
        } else {
            try {
                processVariant(label + " glass", glassUrl, c.getId(), S3_PREFIX_GLASSES, dryRun, stats, false);
                stats.glassProcessed++;
            } catch (Exception e) {
                stats.glassFailed++;
                log.error("{} glass FAILED: {}", label, e.toString());
            }
        }
    }

    /**
     * Download original, generate thumb + detail, then upload (full mode) or
     * write to /tmp (dry-run). Returns nothing — side effects only.
     *
     * @param isCocktail true → updates image_url_thumb/detail; false → glass_image_url_thumb/detail
     */
    private void processVariant(String label, String url, Long cocktailId, String s3Prefix,
                                boolean dryRun, Stats stats, boolean isCocktail) throws IOException, InterruptedException {
        byte[] original = downloadBytes(url);
        stats.bytesIn += original.length;

        String basename = baseNameFromUrl(url);

        byte[] thumbBytes = generator.generate(original, ImageVariantGenerator.Variant.THUMB);
        byte[] detailBytes = generator.generate(original, ImageVariantGenerator.Variant.DETAIL);
        stats.bytesOut += thumbBytes.length + detailBytes.length;

        log.info("{} download: {}KB → thumb: {}KB / detail: {}KB",
                label, original.length / 1024, thumbBytes.length / 1024, detailBytes.length / 1024);

        String thumbKey = s3Prefix + "/" + basename + "_thumb.webp";
        String detailKey = s3Prefix + "/" + basename + "_detail.webp";

        if (dryRun) {
            Path thumbPath = OUT_ROOT.resolve(thumbKey);
            Path detailPath = OUT_ROOT.resolve(detailKey);
            Files.createDirectories(thumbPath.getParent());
            Files.write(thumbPath, thumbBytes);
            Files.write(detailPath, detailBytes);
            log.debug("  wrote {} ({} bytes)", thumbPath, thumbBytes.length);
            log.debug("  wrote {} ({} bytes)", detailPath, detailBytes.length);
            // Skip DB update intentionally; just log what would happen.
            log.info("  (dry-run) would set {} url_thumb=s3://{}/{}",
                    isCocktail ? "image" : "glass_image", props.getBucket(), thumbKey);
            log.info("  (dry-run) would set {} url_detail=s3://{}/{}",
                    isCocktail ? "image" : "glass_image", props.getBucket(), detailKey);
        } else {
            String thumbUrl = uploader.upload(thumbKey, thumbBytes, "image/webp");
            String detailUrl = uploader.upload(detailKey, detailBytes, "image/webp");
            log.info("  uploaded thumb={} detail={}", thumbUrl, detailUrl);
            updateVariantUrls(cocktailId, isCocktail, thumbUrl, detailUrl);
        }
    }

    /**
     * Direct JPQL update (avoids loading & re-saving the full entity graph).
     */
    @Transactional
    protected void updateVariantUrls(Long id, boolean isCocktail, String thumbUrl, String detailUrl) {
        if (isCocktail) {
            em.createQuery(
                    "UPDATE Cocktail c SET c.imageUrlThumb = :thumb, c.imageUrlDetail = :detail WHERE c.id = :id")
                    .setParameter("thumb", thumbUrl)
                    .setParameter("detail", detailUrl)
                    .setParameter("id", id)
                    .executeUpdate();
        } else {
            em.createQuery(
                    "UPDATE Cocktail c SET c.glassImageUrlThumb = :thumb, c.glassImageUrlDetail = :detail WHERE c.id = :id")
                    .setParameter("thumb", thumbUrl)
                    .setParameter("detail", detailUrl)
                    .setParameter("id", id)
                    .executeUpdate();
        }
    }

    // ---------------------------------------------------------------- helpers

    private byte[] downloadBytes(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<byte[]> resp = http.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (resp.statusCode() / 100 != 2) {
            throw new IOException("Download failed status=" + resp.statusCode() + " url=" + url);
        }
        return resp.body();
    }

    /**
     * Derive a deterministic, filesystem-safe basename from the source URL so the
     * generated WebP key is stable across reruns.
     * <pre>
     *   https://onz-cocktail-images.s3.ap-northeast-2.amazonaws.com/cocktails/long_island_iced_tea.png
     *     → long_island_iced_tea
     * </pre>
     */
    static String baseNameFromUrl(String url) {
        String tail = url;
        int q = tail.indexOf('?');
        if (q >= 0) tail = tail.substring(0, q);
        int slash = tail.lastIndexOf('/');
        if (slash >= 0) tail = tail.substring(slash + 1);
        int dot = tail.lastIndexOf('.');
        if (dot > 0) tail = tail.substring(0, dot);
        // sanitize
        tail = tail.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "_");
        if (tail.isBlank()) tail = "img";
        return tail;
    }

    private static String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static class Stats {
        int seen;
        int cocktailProcessed;
        int cocktailSkipped;
        int cocktailFailed;
        int glassProcessed;
        int glassSkipped;
        int glassFailed;
        long bytesIn;
        long bytesOut;
    }
}
