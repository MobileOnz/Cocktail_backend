package com.application.common.auth;

import com.application.common.Constant;
import com.application.common.auth.dto.jwt.JWTStoreDto;
import com.application.common.auth.jwt.JWTRefreshStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class JWTStoreService {
    private final JWTRefreshStore jwtRefreshStore;


    public JWTStoreService(JWTRefreshStore store){
        this.jwtRefreshStore = store;
    }


    public void save(String uuid, String refreshToken){
        jwtRefreshStore.save(uuid, refreshToken);
    }

    public JWTStoreDto findByKey(String uuid){
        return jwtRefreshStore.findByUUID(uuid);
    }

    public List<String> findAllByKey(){
        return jwtRefreshStore.findByAll();
    }

    public void deleteByKey(String uuid){
        jwtRefreshStore.deleteByUUID(uuid);
    }

    public Boolean containKey(String uuid){
        if ( findByKey(uuid) == null ){
            return false;
        }else{
            return true;
        }
    }

    @Scheduled(fixedRate = Constant.REFRESH_EXPIRED_TIME) // 2주관리
    public void isRefreshExpired(){
        jwtRefreshStore.getJwtStore().entrySet().removeIf(entry ->
                Duration.between(entry.getValue().getCreateTime(), LocalDateTime.now()).toMinutes() > 14* 24* 60);
    }

}
