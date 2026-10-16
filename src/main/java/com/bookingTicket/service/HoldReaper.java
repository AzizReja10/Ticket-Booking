package com.bookingTicket.service;

import com.bookingTicket.entities.Event;
import com.bookingTicket.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HoldReaper {
    private final EventRepository eventRepository;
    private final StockService stockService;
    @Scheduled(fixedDelay = 1000)
    public void releaseExpiredHolds()
    {
        for(Event event:eventRepository.findAll())
        {
            long released=stockService.releasedExpired(event.getId());
            if(released>0)
                log.info("event {} : released {} expired holds", event.getId(),released);
        }
    }
}
