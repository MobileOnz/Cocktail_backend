package com.application.domain.bar.service;

import com.application.domain.bar.entity.BarQrPlacard;
import com.application.domain.bar.repository.BarQrPlacardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;

/**
 * QR 서명. **QR = 의도, GPS = 증명.**
 *
 * 인쇄된 플래카드는 회전할 수 없으므로 QR 페이로드는 장기 서명이다. 여기에는 비밀이 없다 —
 * 사진을 찍어 공유해도 GPS 게이트를 통과할 수 없으므로 무용하다.
 * QR 이 증명하는 것은 오직 "이 사람이 저 매장의 플래카드를 봤다"는 **의도**뿐이다.
 *
 * 페이로드 형식:  {slug}.{keyVersion}.{base64url(HMAC-SHA256(secret, "slug|keyVersion"))}
 * 폐기: bar_qr_placard.revoked_at 설정 후 key_version 을 올린 새 플래카드를 발급 → 재출력. 비용 0원.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QrSignatureService {

    private static final String HMAC_ALG = "HmacSHA256";

    private final BarQrPlacardRepository placardRepository;

    /** 관리자 페이지(T-19)가 플래카드 PDF 를 만들 때 쓴다. */
    public String buildPayload(String slug, int keyVersion, String secret) {
        return slug + "." + keyVersion + "." + sign(secret, canonical(slug, keyVersion));
    }

    /**
     * 페이로드를 검증한다. 실패 이유를 구분하지 않고 boolean 만 준다 —
     * 공격자에게 "서명은 맞는데 버전이 틀렸다" 같은 정보를 주지 않기 위해서다.
     *
     * @param slug    경로에서 온 바 slug. 페이로드의 slug 와 반드시 일치해야 한다(다른 매장 QR 차단).
     * @param barId   해당 바의 id
     * @param payload 클라이언트가 스캔해 보낸 문자열. null 이면 false.
     */
    public boolean verify(String slug, Long barId, String payload) {
        if (payload == null || payload.isBlank()) return false;

        String[] parts = payload.trim().split("\\.");
        if (parts.length != 3) return false;

        String payloadSlug = parts[0];
        // 다른 매장의 유효한 QR 을 이 매장에 쓰는 것을 막는다.
        if (!slug.equals(payloadSlug)) return false;

        int keyVersion;
        try {
            keyVersion = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }

        Optional<BarQrPlacard> found = placardRepository.findByBarIdAndKeyVersion(barId, keyVersion);
        if (found.isEmpty() || found.get().isRevoked()) return false;

        String expected = sign(found.get().getSecret(), canonical(payloadSlug, keyVersion));
        return constantTimeEquals(expected, parts[2]);
    }

    private String canonical(String slug, int keyVersion) {
        return slug + "|" + keyVersion;
    }

    private String sign(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALG));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) {
            throw new IllegalStateException("QR 서명 생성 실패", e);
        }
    }

    /** 타이밍 공격 방어. String.equals 는 첫 불일치에서 즉시 반환한다. */
    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
