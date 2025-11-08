package com.application.web.services.bookmark;

import com.application.common.auth.jwt.JWTUtil;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@SpringBootTest
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@ActiveProfiles("test")
public class JwtTest {

    private final JWTUtil jwtUtil;

    public JwtTest(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Test
    void generateTestAccessToken() {
        String token = jwtUtil.createAccessJwt(
                UUID.randomUUID().toString(),
                "test-user-id",
                "USER"
        );

        System.out.println("🟢 테스트용 Access Token:");
        System.out.println(token);
    }

}
