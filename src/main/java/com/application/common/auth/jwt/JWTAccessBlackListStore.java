package com.application.common.auth.jwt;

import com.application.common.auth.dto.jwt.JWTAccessBlackListDto;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JWTAccessBlackListStore {
    private static Map<String, JWTAccessBlackListDto> store = new ConcurrentHashMap<>();


    public void save(String uuid, String jwt){
        store.put(uuid, new JWTAccessBlackListDto(jwt));
    }

    public void deleteByid(String uuid){
        store.remove(uuid);
    }

    public String getAccessTokenByUUID(String uuid){
        JWTAccessBlackListDto dto = store.get(uuid);
        return (dto != null) ? dto.getAccessToken() : null;
    }

    public JWTAccessBlackListDto getBlackListDtoByUUID(String uuid){
        return store.get(uuid);
    }

    public List<String> findAllKeySet(){
        return store.keySet().stream().toList();
    }


}
