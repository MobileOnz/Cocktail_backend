package com.application.web.services.auth;

import com.application.common.Constant;
import com.application.common.auth.dto.jwt.JWTAccessBlackListDto;
import com.application.common.auth.jwt.JWTAccessBlackListStore;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JWTAccessTokenBlackListService {
    private final JWTAccessBlackListStore jwtAccessBlackListStore;

    public void addBlackList(String uuid, String token){
        jwtAccessBlackListStore.save(uuid, token);
    }

    public void deleteAccessTokeFromBlackList(String uuid){
        jwtAccessBlackListStore.deleteByid(uuid);
    }

    public String getAccessTokenFromBlackList(String uuid){
        return jwtAccessBlackListStore.getAccessTokenByUUID(uuid);
    }


    @Scheduled(fixedRate = Constant.BLACKLIST_EXPIRED_TIME)
    public void blackListExpired(){

        List<String> keys = jwtAccessBlackListStore.findAllKeySet();
        for (String key : keys) {
            JWTAccessBlackListDto dto =  jwtAccessBlackListStore.getBlackListDtoByUUID(key);
            LocalDateTime startTime = dto.getCreateTime();
            Duration duration = Duration.between(startTime, LocalDateTime.now());

            if(duration.toMinutes() > 60){
                deleteAccessTokeFromBlackList(key);
            }

        }
    }
}
