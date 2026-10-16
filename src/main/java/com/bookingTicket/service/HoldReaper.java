package com.bookingTicket.service;

import com.bookingTicket.entities.Event;
import com.bookingTicket.exceptions.RequestInProgressException;
import com.bookingTicket.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class HoldReaper {
    private final EventRepository eventRepository;
    private final StockService stockService;
    private final RedisLockService lockService;
    @Scheduled(fixedDelay = 1000)
    public void releaseExpiredHolds()
    {
       if(lockService.tryLock("reaper", Duration.ofMillis(900))==null)
           return;
       for(Event e:eventRepository.findAll())
           {
               long released=stockService.releaseExpired(e.getId());
               if(released>0)
                   log.info("event {}: released {} expired holds",e.getId(),released);
           }
    }
    @ExceptionHandler(RequestInProgressException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleInProgress(RequestInProgressException e)
    { return e.getMessage(); }
}
