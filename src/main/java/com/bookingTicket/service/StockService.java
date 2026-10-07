package com.bookingTicket.service;

import com.bookingTicket.exceptions.AlreadyBookedException;
import com.bookingTicket.exceptions.EventNotReadyException;
import com.bookingTicket.exceptions.SoldOutException;
import jakarta.validation.groups.Default;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockService {
    private final StringRedisTemplate redis;

    public StockService(StringRedisTemplate redis) {
        this.redis = redis;
    }
    private DefaultRedisScript<Long> reserveScript;
    @jakarta.annotation.PostConstruct
    void loadScript()
    {
        reserveScript=new DefaultRedisScript<>();
        reserveScript.setLocation(new ClassPathResource("scripts/reserve_seat.lua"));
        reserveScript.setResultType(Long.class);
    }
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
}
