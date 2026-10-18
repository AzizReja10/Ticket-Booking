package com.bookingTicket;


import com.bookingTicket.entities.Booking;
import com.bookingTicket.enums.BookingStatus;
import com.bookingTicket.repository.BookingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BookingConstraintTest {

    @Autowired
    BookingRepository bookingRepository;

    private Booking booking(long eventId, String userId) {
        return Booking.builder()
                .eventId(eventId)
                .userId(userId)
                .status(BookingStatus.CONFIRMED)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void sameUserCannotHaveTwoBookingsForOneEvent() {
        long eventId = System.nanoTime();   // unique, so reruns never collide

        bookingRepository.saveAndFlush(booking(eventId, "alice"));

        assertThrows(DataIntegrityViolationException.class,
                () -> bookingRepository.saveAndFlush(booking(eventId, "alice")));

        assertEquals(1, bookingRepository.countByEventId(eventId));
    }

    @Test
    void sameUserCanBookDifferentEvents() {
        long base = System.nanoTime();

        bookingRepository.saveAndFlush(booking(base, "bob"));
        assertDoesNotThrow(() -> bookingRepository.saveAndFlush(booking(base + 1, "bob")));
    }
}
