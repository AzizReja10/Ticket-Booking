package com.bookingTicket.service;

import com.bookingTicket.exceptions.AlreadyBookedException;
import com.bookingTicket.exceptions.EventNotReadyException;
import com.bookingTicket.exceptions.HoldExpiredException;
import com.bookingTicket.exceptions.SoldOutException;
import jakarta.validation.groups.Default;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.aspectj.weaver.tools.cache.SimpleCacheFactory.path;

@Service
public class StockService {
    private final StringRedisTemplate redis;
    private DefaultRedisScript<Long> holdScript;
    private DefaultRedisScript<Long> confirmScript;
    private DefaultRedisScript<Long> releaseExpiredScript;
    private String holdsKey(Long eventId)
    {
        return "event: "+eventId+" :holds";
    }
    private DefaultRedisScript<Long> load(String path)
    {
        DefaultRedisScript<Long> s=new DefaultRedisScript<>();
        s.setLocation((new ClassPathResource(path)));
        s.setResultType(Long.class);
        return s;
    }
    @jakarta.annotation.PostConstruct
    void loadScript()
    {
        reserveScript = load("scripts/reserve_seat.lua");
        holdScript = load("scripts/hold_seat.lua");
        confirmScript = load("scripts/confirm_hold.lua");
        releaseExpiredScript = load("scripts/release_expired.lua");
    }
    public StockService(StringRedisTemplate redis) {
        this.redis = redis;
    }
    private DefaultRedisScript<Long> reserveScript;
    private String stockKey(Long eventId)
    {
        return "event:"+eventId+":stock";
    }
    private String usersKey(Long eventId)
    {
        return "event:"+eventId+":users";
    }
    public void initStock(Long eventId,int seats)
    {
        redis.opsForValue().set(stockKey(eventId),String.valueOf(seats));
        redis.delete(usersKey(eventId));
        redis.delete(holdsKey(eventId));
    }
    public long reserve(Long eventId,String userId)
    {
        Long result=redis.execute(
                reserveScript,
                List.of(stockKey(eventId),usersKey(eventId)),userId);
        if (result == null) throw new IllegalStateException("Redis returned null");
        if (result == -1) throw new SoldOutException();
        if (result == -2) throw new AlreadyBookedException();
        if (result == -3) throw new EventNotReadyException();
        return result;
    }
    public void release(Long eventId,String userId)
    {
        redis.opsForValue().increment((stockKey(eventId)));
        redis.opsForSet().remove(usersKey(eventId),userId);
    }
    public long hold(Long eventId,String userId,Long ttlMs)
    {
        Long result=redis.execute(
                holdScript,
                List.of(stockKey(eventId), usersKey(eventId), holdsKey(eventId)),
                userId, String.valueOf(ttlMs)
        );
        if(result==null)
            throw new IllegalStateException("redis returned null");
        if (result == -1)
            throw new SoldOutException();
        if (result == -2)
            throw new AlreadyBookedException();
        if (result == -3)
            throw new EventNotReadyException();
        return result;
    }
    public void confirmHold(Long eventId,String userId)
    {
        Long result=redis.execute(confirmScript,List.of(holdsKey(eventId)),userId);
        System.out.println("CONFIRM RESULT = " + result);
        if(result==null||result<0)
            throw new HoldExpiredException();
    }
    public Long releaseExpired(Long eventId)
    {
        Long n=redis.execute(releaseExpiredScript,
                List.of(stockKey(eventId),usersKey(eventId),holdsKey(eventId))
                );
        return n==null?0:n;
    }
}
