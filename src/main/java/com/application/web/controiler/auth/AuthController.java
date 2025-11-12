package com.application.web.controiler.auth;


import com.application.common.Constant;
import com.application.common.auth.dto.login.ReqSignupDto;
import com.application.common.auth.dto.login.ReqSocialLoginDto;
import com.application.common.auth.dto.login.ResSocialLoginDto;
import com.application.common.auth.dto.login.ResTokenDto;
import com.application.common.exception.custom.CustomApiException;
import com.application.web.services.auth.OAuth2Service;
import com.application.common.response.ResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;


@Slf4j
//@RestController
@RequiredArgsConstructor
public class AuthController {

    private final OAuth2Service oAuth2Service;

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<?> reissueRefreshToken(HttpServletRequest request){
        String refreshToken = request.getHeader("Refresh-Token");
        ResTokenDto resTokenDto = oAuth2Service.reissueRefreshToken(refreshToken);
        return new ResponseEntity<>(new ResponseDto<>(Constant.SUCCESS_CODE, "access token and refersh token reissue.", resTokenDto), HttpStatus.OK);
    }

    @PostMapping("/api/auth/social-login")
    public ResponseEntity<?> socialLogin(@Valid @RequestBody ReqSocialLoginDto reqSocialLoginDto, BindingResult bindingResult){
        log.info("[INFO] (SOCIAL LOGIN DTO) \n {}", reqSocialLoginDto.toString());
        ResSocialLoginDto resSocialLoginDto = oAuth2Service.socialLogin(reqSocialLoginDto);
        return new ResponseEntity<>(new ResponseDto<>(Constant.SUCCESS_CODE, "success", resSocialLoginDto), HttpStatus.CREATED);
    }

    @PostMapping("/api/auth/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody ReqSignupDto reqSignupDto, BindingResult bindingResult){

        log.info("reqSigupDto :{}",reqSignupDto.toString() );
        if(!reqSignupDto.getAgeTerm()){
            throw new CustomApiException("만 14세 이상 동의는 필수입니다.");
        }

        if(!reqSignupDto.getServiceTerm()){
            throw new CustomApiException("서비스 이용약관 동의는 필수입니다.");
        }

        ResSocialLoginDto resSocialLoginDto = oAuth2Service.signup(reqSignupDto);
        return new ResponseEntity<>(new ResponseDto<>(Constant.SUCCESS_CODE, "success", resSocialLoginDto),HttpStatus.CREATED);
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request){
        oAuth2Service.logout(request.getHeader("Authorization"));
        return new ResponseEntity<>(new ResponseDto<>(Constant.SUCCESS_CODE, "logout", null), HttpStatus.OK);
    }


}
