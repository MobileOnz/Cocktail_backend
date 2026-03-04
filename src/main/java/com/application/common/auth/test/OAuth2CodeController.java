package com.application.common.auth.test;

import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;


@Controller
public class OAuth2CodeController {

    private final RestTemplate restTemplate;

    public OAuth2CodeController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // 네이버 AccessToken 요청
    @GetMapping("/login/oauth2/code/naver")
    public ResponseEntity<?> getNaverAccessToken(@RequestParam("code") String code,
                                                   @RequestParam("state") String state) {

        Map<String, String> body = new HashMap<>();
        body.put("provider", "naver");
        body.put("code", code);
        body.put("state", state);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

//        String url = "http://localhost:8080/api/auth/social-login";
        String url = "http://onz-cocktail.kr/onz/api/auth/social-login";
        ResponseEntity<Map> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

        return ResponseEntity.status(responseEntity.getStatusCode()).body(responseEntity.getBody());
    }

    // 구글 AccessToken 요청
    @GetMapping("/login/oauth2/code/google")
    public ResponseEntity<?> getGoogleAccessToken(@RequestParam("code") String code) {

        Map<String, String> body = new HashMap<>();
        body.put("code", code);
        body.put("provider", "google");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);


        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

//        String url = "http://localhost:8080/api/auth/social-login";
        String url = "http://onz-cocktail.kr/onz/api/auth/social-login";
        ResponseEntity<Map> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

        return ResponseEntity.status(responseEntity.getStatusCode()).body(responseEntity.getBody());
    }

    @GetMapping("/login/oauth2/code/kakao")
    public ResponseEntity<?> getKakaoAccessToken(@RequestParam("code") String code) {

        Map<String, String> body = new HashMap<>();
        body.put("code", code);
        body.put("provider", "kakao");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);


        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

//        String url = "http://localhost:8080/api/auth/social-login";
        String url = "http://onz-cocktail.kr/onz/api/auth/social-login";
        ResponseEntity<Map> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, Map.class);

        return ResponseEntity.status(responseEntity.getStatusCode()).body(responseEntity.getBody());
    }
}
