package com.bookingTicket.service;

import com.bookingTicket.entities.Booking;
import com.bookingTicket.entities.Event;
import com.bookingTicket.enums.BookingStatus;
import com.bookingTicket.exceptions.SoldOutException;
import com.bookingTicket.repository.BookingRepository;
import com.bookingTicket.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class NaiveBookingsService {
    private final EventRepository eventRepository;
    private final BookingRepository bookingRepository;

    public NaiveBookingsService(EventRepository eventRepository, BookingRepository bookingRepository) {
        this.eventRepository = eventRepository;
        this.bookingRepository = bookingRepository;
    }
    @Transactional
    public Booking book(Long eventId,String userId)
    {
        Event event=eventRepository.findById(eventId).orElseThrow(()->new IllegalArgumentException("Event not found"));
        if(event.getAvailableSeats()<=0)
            throw new SoldOutException();
        event.setAvailableSeats((event.getAvailableSeats()-1));
        eventRepository.save(event);
        return bookingRepository.save(Booking.builder()
                        .eventId(eventId)
                        .userId(userId)
                        .status(BookingStatus.CONFIRMED)
                        .createdAt(Instant.now())
                        .build()
                );
    }
}
