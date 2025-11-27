package com.application.common.auth.strategy;

import com.application.common.auth.config.OAuth2PropertiesValue;
import com.application.common.exception.custom.CustomApiException;
import com.application.domain.member.entity.ParsedMember;
import com.application.domain.member.enums.Role;
import com.application.domain.member.enums.SocialLogin;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppleLoginStrategy implements SocialLoginStrategy{

    private final RestTemplate restTemplate;
    private final OAuth2PropertiesValue oAuth2PropertiesValue;


    //User 정보 추출
    @Override
    public Map<String, Object> getUserInfo(String idToken) {

        RSAPublicKey publicKey = getApplePublicKey(idToken);

        Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(idToken)
                .getPayload();

        String sub = claims.get("sub", String.class);
        String email = claims.get("email", String.class);

        if(sub == null || sub.isEmpty()){
            log.error("Apple 식별자 값이 존재하지 않습니다. sub : {}", sub);
            throw new CustomApiException("[소셜로그인 실패] 애플 통신 오류");
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("sub", sub);
        userInfo.put("email", email);

        return userInfo;
    }

    //access Token 발급
    @Override
    public String getAccessToken(String code, String state) {

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", oAuth2PropertiesValue.getAppleClientId());
        params.add("client_secret", generateKey());
        params.add("grant_type", "authorization_code");
        params.add("code", code);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        ResponseEntity<AppleResponseDto> response = restTemplate.exchange(
                oAuth2PropertiesValue.getAppleTokenUrl(),
                HttpMethod.POST,
                request,
                AppleResponseDto.class
        );


        return Objects.requireNonNull(response.getBody()).getId_token();
    }

    //USER 식별 ID 값 추출 (provider+식별값)
    @Override
    public String getProviderId(Map<String, Object> userInfo) {
        return "apple"+userInfo.get("sub").toString();
    }

    //ParsedMember Dto 사용 (이름, 이메일값)
    @Override
    public ParsedMember parse(Map<String, Object> userInfo) {

        String email = Optional.ofNullable(userInfo.get("email"))
                .map(Object::toString)
                .orElse("onz@apple.com");

        String displayName = email.contains("@") ? email.split("@")[0] : "onz";


        return ParsedMember.builder()
                .credentialId(getProviderId(userInfo))
                .name(displayName)
                .email(email)
                .role(Role.USER)
                .socialLogin(SocialLogin.APPLE)
                .build();
    }

    public RSAPublicKey getApplePublicKey(String token){

        try {
            String[] parts = token.split("\\.");
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode header = objectMapper.readTree(headerJson);

            String kid = header.get("kid").asText();
            String alg = header.get("alg").asText();
            log.info("[Apple 로그인 시도] 공개키 검증  - kid: " + kid + ", alg: " + alg);

            ResponseEntity<String> response = restTemplate.getForEntity(oAuth2PropertiesValue.getApplePublicKeyUrl(), String.class);
            JsonNode keys = objectMapper.readTree(response.getBody()).get("keys");

            for(JsonNode key : keys){
                if(kid.equals(key.get("kid").asText()) && alg.equals(key.get("alg").asText())){
                    byte[] nBytes = Base64.getUrlDecoder().decode(key.get("n").asText());
                    byte[] eBytes = Base64.getUrlDecoder().decode(key.get("e").asText());

                    BigInteger modulus = new BigInteger(1, nBytes);
                    BigInteger exponent = new BigInteger(1, eBytes);

                    RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
                    return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
                }
            }
        } catch (JsonProcessingException | NoSuchAlgorithmException |InvalidKeySpecException e) {
            log.error("[Apple 로그인 실패]", e);
            throw new CustomApiException("[Exception] Apple 로그인 실패");
        }

        log.error("[Apple 로그인 실패] : 맞는 공개키값이 존재하지 않습니다.");
        throw new CustomApiException("[Exception] Apple 로그인 실패 : 공개키");
    }

    public String generateKey(){
        String teamId = oAuth2PropertiesValue.getAppleTeamId();
        String clientId = oAuth2PropertiesValue.getAppleClientId();
        String keyId = oAuth2PropertiesValue.getAppleKeyId();
        String privateKeyPem = oAuth2PropertiesValue.getAppleKey();

        Instant now = Instant.now();
        Instant expiresAt = now.plus(180, ChronoUnit.DAYS); // 최대 6개월 유효

        return Jwts.builder()
                .header().keyId(keyId).and()
                .issuer(teamId)
                .subject(clientId)
                .audience().add(oAuth2PropertiesValue.getAppleAudience()).and()
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(loadECPrivateKey(privateKeyPem))
                .compact();
    }


    public PrivateKey loadECPrivateKey(String privateKeyPem){
        try{
            String privateKeyContent = privateKeyPem.replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

            return KeyFactory.getInstance("EC").generatePrivate(keySpec);

        }catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            log.error("[Apple 로그인 실패] private Key 추출 실패", e);
            throw new CustomApiException("[Apple 로그인 실패] 관리자 문의 바랍니다.");
        }
    }

    @Data
    public static class AppleResponseDto{
        private String access_token;
        private String expires_in;
        private String id_token;
        private String refresh_token;
        private String token_type;
    }
}
