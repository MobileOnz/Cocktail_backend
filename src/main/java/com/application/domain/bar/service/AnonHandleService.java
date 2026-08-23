package com.application.domain.bar.service;

import com.application.domain.bar.entity.BarChatIdentity;
import com.application.domain.bar.repository.BarChatIdentityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;

/**
 * 익명 핸들 생성.
 *
 *   authorRef = HMAC-SHA256(secret, "{memberId}|{barId}|{yyyy-MM-dd(KST)}")[:32]
 *
 * 성질:
 *   - 바가 다르면 다른 핸들      → 크로스-바 추적 불가
 *   - 날짜가 다르면 다른 핸들    → 장기 프로파일링 불가 (채팅 06:00 리셋 주기와 일치)
 *   - 같은 날 같은 바에서는 동일 → 대화 맥락 유지 + 차단 기능 성립
 *   - HMAC 이므로 역산 불가      → identity 매핑표가 파기되면 메시지는 수학적으로 익명
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnonHandleService {

    private static final String HMAC_ALG = "HmacSHA256";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 이 값이 바뀌면 전원의 핸들이 바뀐다. 절대 로그에 남기지 말 것. */
    @Value("${onz.chat.handle-secret:local-dev-chat-handle-secret-change-me}")
    private String handleSecret;

    private static final String[] ADJECTIVES = {
            "조용한", "은은한", "차분한", "시크한", "느긋한", "포근한", "산뜻한", "묵직한",
            "달콤한", "쌉쌀한", "청량한", "고요한", "나른한", "우아한", "다정한", "덤덤한",
            "산만한", "노곤한", "말간", "잔잔한", "부드러운", "짜릿한", "선선한", "촉촉한",
            "새콤한", "향긋한", "깔끔한", "진득한", "훈훈한", "샛노란", "그윽한", "정갈한",
            "발랄한", "무던한", "느슨한", "단단한", "투명한", "따스한", "서늘한", "가벼운"
    };

    private static final String[] NOUNS = {
            "마티니", "네그로니", "모히토", "하이볼", "다이키리", "마가리타", "올드패션", "맨해튼",
            "진토닉", "김렛", "카이피리냐", "벨리니", "미모사", "블러디메리", "코스모폴리탄", "사이드카",
            "위스키사워", "민트줄렙", "아메리카노", "스프리츠", "쿠바리브레", "롱아일랜드", "피나콜라다", "준벅",
            "갓파더", "화이트러시안", "블랙러시안", "가을소나기", "샴페인", "베스퍼",
            "라스티네일", "브랜디알렉산더", "핑크레이디", "사제락", "톰콜린스", "페이퍼플레인",
            "펜니실린", "라모스피즈", "에스프레소마티니", "아페롤"
    };

    private final BarChatIdentityRepository identityRepository;

    /** 오늘(KST) 이 회원의 이 바에서의 신원을 가져오거나 만든다. */
    @Transactional
    public BarChatIdentity resolve(Long memberId, Long barId) {
        LocalDate epoch = LocalDate.now(KST);
        String authorRef = computeAuthorRef(memberId, barId, epoch);

        return identityRepository.findById(authorRef)
                .orElseGet(() -> identityRepository.save(
                        BarChatIdentity.of(authorRef, memberId, barId, epoch, nicknameFor(authorRef))));
    }

    String computeAuthorRef(Long memberId, Long barId, LocalDate epoch) {
        String data = memberId + "|" + barId + "|" + epoch;
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(handleSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALG));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw).substring(0, 32);
        } catch (Exception e) {
            throw new IllegalStateException("익명 핸들 생성 실패", e);
        }
    }

    /** 핸들에서 결정적으로 파생. 같은 핸들 → 항상 같은 닉네임. */
    String nicknameFor(String authorRef) {
        long h = Long.parseUnsignedLong(authorRef.substring(0, 12), 16);
        String adj = ADJECTIVES[(int) Math.floorMod(h, ADJECTIVES.length)];
        String noun = NOUNS[(int) Math.floorMod(h >>> 8, NOUNS.length)];
        int num = (int) Math.floorMod(h >>> 16, 100);
        return adj + noun + num;
    }
}
