package com.bookingTicket.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisLockService {
    private final StringRedisTemplate redis;
    private DefaultRedisScript<Long> unlockScript;
    @PostConstruct
    void init()
    {
        unlockScript=new DefaultRedisScript<>();
        unlockScript.setLocation(new ClassPathResource("scripts/unlock.lua"));
        unlockScript.setResultType(Long.class);
    }
    public String tryLock(String name, Duration ttl)
    {
        String token= UUID.randomUUID().toString();
        Boolean ok=redis.opsForValue().setIfAbsent("lock:"+name,token,ttl);
        return Boolean.TRUE.equals(ok)?token:null;
    }
    public boolean unlock(String name,String token)
    {
        Long n=redis.execute(unlockScript, List.of("lock:"+name),token);
        return n!=null &&n==1;
    }
}
