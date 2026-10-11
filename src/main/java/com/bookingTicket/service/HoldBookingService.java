package com.bookingTicket.service;
import com.bookingTicket.entities.Booking;
import com.bookingTicket.enums.BookingStatus;
import com.bookingTicket.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class HoldBookingService {
    private final StockService stockService;
    private final BookingRepository bookingRepository;
    @Value("${booking.hold-ttl-seconds}")
    private Long holdTtlSeconds;
    private long hold(Long eventId,String userId)
    {
        return stockService.hold(eventId,userId,holdTtlSeconds*1000);
    }
    public Booking confirm(Long eventId, String userId)
    {
        stockService.confirmHold(eventId,userId);
        try
        {
            return bookingRepository.save(Booking.builder()
                    .eventId(eventId)
                    .userId(userId)
                    .status(BookingStatus.CONFIRMED)
                    .createdAt(Instant.now())
                    .build()
            );
        }catch(RuntimeException e)
        {
            stockService.release(eventId,userId);
            throw e;
        }
    }
}
