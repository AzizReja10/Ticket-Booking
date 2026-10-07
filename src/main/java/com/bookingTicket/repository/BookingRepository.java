package com.bookingTicket.repository;

import com.bookingTicket.entities.Booking;
import com.bookingTicket.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface BookingRepository extends JpaRepository<Booking,Long> {
    long countByEventIdAndStatus(Long eventId, BookingStatus status);
    long countByEventId(Long eventId);
    boolean existsByEventIdAndUserIdAndStatusIn(Long eventId,String userId,java.util.Collection<BookingStatus> statuses);
}
