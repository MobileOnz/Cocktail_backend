package com.application.common.auth.jwt;

import com.application.common.Constant;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JWTUtil {

    private SecretKey secretKey;

    public JWTUtil(@Value ("${spring.jwt.secret}") String secret){
        secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    public String getUUID(String token){
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("uuid", String.class);
    }

    public String getCredentialId(String token){
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("credentialId", String.class);
    }

    public String getRole(String token){
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
    }

    public Boolean isAccessExpired(String token){
        try {
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            // 토큰이 이미 만료된 경우
            return true;
        } catch (Exception e) {
            // 기타 예외는 토큰 유효하지 않은 것으로 처리
            return true;
        }
    }

    /**
     * 401 원인을 로그로 구분하기 위해 Access Token 검증 실패 사유만 판별한다.
     * 만료(정상적인 갱신 대상)와 서명 불일치·손상(시크릿 변경, 위조 의심)을 운영에서 나눠 보기 위함이다.
     */
    public String getAccessTokenFailureReason(String token){
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return "VALID";
        } catch (ExpiredJwtException e) {
            return "EXPIRED";
        } catch (Exception e) {
            return "INVALID";
        }
    }

    public Boolean isRefreshExpired(String token){
        try {
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            // 토큰이 이미 만료된 경우
            return true;
        } catch (Exception e) {
            // 기타 예외는 토큰 유효하지 않은 것으로 처리
            return true;
        }
    }

    public String createAccessJwt(String uuid, String credentialId, String role){
        return Jwts.builder()
                .claim("uuid", uuid)
                .claim("credentialId", credentialId)
                .claim("role", role)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + Constant.ACCESS_TOKEN_EXPIRATION))
                .signWith(secretKey)
                .compact();

    }

    public String createRefreshJwt(String uuid, String credentialId, String role){

        return Jwts.builder()
                .claim("uuid", uuid)
                .claim("credentialId", credentialId)
                .claim("role", role)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + Constant.REFRESH_TOKEN_EXPIRATION))
                .signWith(secretKey)
                .compact();

    }
}
