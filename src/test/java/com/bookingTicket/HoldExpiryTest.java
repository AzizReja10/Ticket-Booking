package com.bookingTicket;
import com.bookingTicket.entities.Event;
import com.bookingTicket.exceptions.HoldExpiredException;
import com.bookingTicket.exceptions.SoldOutException;
import com.bookingTicket.repository.BookingRepository;
import com.bookingTicket.repository.EventRepository;
import com.bookingTicket.service.HoldBookingService;
import com.bookingTicket.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties = "booking.hold-ttl-seconds=2")
public class HoldExpiryTest {
    @Autowired HoldBookingService holdService;
    @Autowired StockService stockService;
    @Autowired EventRepository eventRepository;
    @Autowired BookingRepository bookingRepository;
    private Event newEvent(int seats)
    {
        Event e= eventRepository.save(Event.builder()
                .name("Test")
                .totalSeats(seats)
                .availableSeats(seats)
                .build());
        stockService.initStock(e.getId(),seats);
        return e;
    }
    @Test
    void expiredHoldsReturnSeatsToPool() throws Exception
    {
        Event e=newEvent(2);
        holdService.hold(e.getId(),"aziz");
        holdService.hold(e.getId(),"bob");
        assertThrows(SoldOutException.class,()->holdService.hold(e.getId(),"carol"));
        Thread.sleep(4000);
        assertDoesNotThrow(()->holdService.hold(e.getId(),"carol"));
    }
    @Test
    void confirmBeforeExpiryCreatedBooking()
    {
        Event e=newEvent(1);
        holdService.hold(e.getId(),"aziz");
        holdService.confirm(e.getId(),"aziz");
        assertEquals(1,bookingRepository.countByEventId(e.getId()));
    }
    @Test
    void confirmAfterExpiryFails() throws Exception
    {
        Event e=newEvent(1);
        holdService.hold(e.getId(),"aziz");
        Thread.sleep(3000);
        assertThrows(HoldExpiredException.class,()->holdService.confirm(e.getId(),"aziz"));
        assertEquals(0,bookingRepository.countByEventId(e.getId()));
    }
}
