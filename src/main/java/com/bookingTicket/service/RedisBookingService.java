package com.bookingTicket.service;

import com.bookingTicket.entities.Booking;
import com.bookingTicket.enums.BookingStatus;
import com.bookingTicket.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class RedisBookingService {
    private final StockService stockService;
    private final BookingRepository bookingRepository;

    public RedisBookingService(StockService stockService, BookingRepository bookingRepository) {
        this.stockService = stockService;
        this.bookingRepository = bookingRepository;
    }
    public Booking book(Long eventId,String userId)
    {
        stockService.reserve(eventId,userId);
        try
        {
            return  bookingRepository.save(Booking.builder()
                    .eventId(eventId)
                    .userId(userId)
                    .status(BookingStatus.CONFIRMED)
                    .createdAt(Instant.now())
                    .build()
            );
        }catch (RuntimeException e)
        {
            stockService.release(eventId, userId);
            throw e;
        }
    }
}
