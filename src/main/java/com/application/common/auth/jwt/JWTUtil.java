package com.application.common.auth.jwt;

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
                .expiration(new Date(System.currentTimeMillis()+ 60 * 60 * 1000L))
                .signWith(secretKey)
                .compact();

    }

    public String createRefreshJwt(String uuid, String credentialId, String role){

        return Jwts.builder()
                .claim("uuid", uuid)
                .claim("credentialId", credentialId)
                .claim("role", role)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis()+ 14 * 24 * 60 * 60 * 1000L))
                .signWith(secretKey)
                .compact();

    }
}
