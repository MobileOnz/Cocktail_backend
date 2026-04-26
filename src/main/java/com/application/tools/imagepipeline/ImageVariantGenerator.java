package com.application.tools.imagepipeline;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Pure utility: takes original image bytes (PNG/JPEG/...) and produces a WebP
 * variant capped at a max dimension while preserving aspect ratio.
 *
 * <p>Resizing uses Thumbnailator (pure-java). WebP encoding uses
 * {@code org.sejda.imageio:webp-imageio} which auto-registers an ImageIO SPI.
 */
@Slf4j
@Component
public class ImageVariantGenerator {

    public enum Variant {
        THUMB(400, 0.80f),
        DETAIL(1200, 0.80f);

        public final int maxDim;
        public final float quality;

        Variant(int maxDim, float quality) {
            this.maxDim = maxDim;
            this.quality = quality;
        }

        public String suffix() {
            return name().toLowerCase();
        }
    }

    /**
     * Resize the image so that the longer side is at most {@code variant.maxDim}
     * (no upscaling) and encode to WebP at {@code variant.quality}.
     *
     * @return WebP-encoded byte array
     */
    public byte[] generate(byte[] originalBytes, Variant variant) throws IOException {
        if (originalBytes == null || originalBytes.length == 0) {
            throw new IOException("originalBytes is empty");
        }

        BufferedImage source = ImageIO.read(new ByteArrayInputStream(originalBytes));
        if (source == null) {
            throw new IOException("Could not decode source image (unknown format)");
        }

        int srcW = source.getWidth();
        int srcH = source.getHeight();
        int maxDim = variant.maxDim;

        // Preserve aspect ratio, never upscale.
        BufferedImage resized;
        if (srcW <= maxDim && srcH <= maxDim) {
            resized = ensureRgb(source);
        } else {
            BufferedImage scaled = Thumbnails.of(source)
                    .size(maxDim, maxDim)        // bounding box
                    .keepAspectRatio(true)
                    .asBufferedImage();
            resized = ensureRgb(scaled);
        }

        return encodeWebp(resized, variant.quality);
    }

    /**
     * The sejda WebP encoder requires TYPE_INT_RGB (no alpha). PNGs commonly come
     * in as TYPE_4BYTE_ABGR / TYPE_INT_ARGB which would otherwise blow up. We
     * paint onto a white background to flatten any alpha channel.
     */
    private BufferedImage ensureRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, src.getWidth(), src.getHeight());
            g.drawImage(src, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }

    private byte[] encodeWebp(BufferedImage image, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByMIMEType("image/webp");
        if (!writers.hasNext()) {
            throw new IOException("No WebP ImageWriter found on classpath. "
                    + "Make sure org.sejda.imageio:webp-imageio is on the runtime classpath.");
        }
        ImageWriter writer = writers.next();

        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        // sejda's WebP writer exposes "Lossy" / "Lossless"
        try {
            params.setCompressionType("Lossy");
        } catch (IllegalArgumentException ignore) {
            // older versions only accept defaults — fall through
        }
        params.setCompressionQuality(quality);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ImageOutputStream ios = new MemoryCacheImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }
}
