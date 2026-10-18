package com.bookingTicket.service;
import com.bookingTicket.entities.Booking;
import com.bookingTicket.enums.BookingStatus;
import com.bookingTicket.exceptions.RequestInProgressException;
import com.bookingTicket.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class HoldBookingService {
    private final StockService stockService;
    private final RedisLockService lockService;
    private final BookingRepository bookingRepository;
    @Value("${booking.hold-ttl-seconds}")
    private Long holdTtlSeconds;
    public long hold(Long eventId, String userId)
    {
        return stockService.hold(eventId,userId,holdTtlSeconds*1000);
    }
    public Booking confirm(Long eventId, String userId)
    {
        String lockName="confirm:"+eventId+":"+userId;
        String token=lockService.tryLock(lockName, Duration.ofSeconds(10));
        if(token==null)
            throw new RequestInProgressException();
        try
        {
            var existing=bookingRepository.findFirstByEventIdAndUserIdAndStatus(eventId,userId,BookingStatus.CONFIRMED);
            if(existing.isPresent())
            {
                return existing.get();
            }
            stockService.confirmHold(eventId,userId);
            try
            {
                return  bookingRepository.save(Booking.builder()
                        .eventId(eventId)
                        .userId(userId)
                        .status(BookingStatus.CONFIRMED)
                        .createdAt(Instant.now())
                        .build());
            } catch (DataIntegrityViolationException e) {
                return bookingRepository
                        .findFirstByEventIdAndUserIdAndStatus(eventId, userId, BookingStatus.CONFIRMED)
                        .orElseThrow(() -> e);
            }
        }catch (RuntimeException e) {
            stockService.release(eventId, userId);
            throw e;
        }
        finally {
            lockService.unlock(lockName,token);
        }
    }
}
