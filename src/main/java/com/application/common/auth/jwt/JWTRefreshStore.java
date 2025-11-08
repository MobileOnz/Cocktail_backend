package com.application.common.auth.jwt;

import com.application.common.auth.dto.jwt.JWTStoreDto;
import lombok.Getter;
import org.springframework.stereotype.Component;


import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Getter
public class JWTRefreshStore {

    private Map<String, JWTStoreDto> jwtStore = new ConcurrentHashMap<>();
    private String uuid = null;


    public void save(String uuid, String refreshToken){
        JWTStoreDto jwtStoreDto = new JWTStoreDto(refreshToken);
        jwtStore.put(uuid, jwtStoreDto);
    }

    public JWTStoreDto findByUUID(String uuid){
        return jwtStore.get(uuid);
    }

    public List<String> findByAll(){
        return jwtStore.keySet().stream().toList();
    }

    public void deleteByUUID(String uuid){
            jwtStore.remove(uuid);
    }

    public void clear(){
        jwtStore.clear();
    }



}
