package com.bookingTicket.controller;

import com.bookingTicket.entities.Booking;
import com.bookingTicket.entities.Event;
import com.bookingTicket.exceptions.AlreadyBookedException;
import com.bookingTicket.exceptions.SoldOutException;
import com.bookingTicket.repository.EventRepository;
import com.bookingTicket.service.NaiveBookingsService;
import com.bookingTicket.service.RedisBookingService;
import com.bookingTicket.service.StockService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BookingController {
    private final StockService stockService;
    private final RedisBookingService redisBookingService;
    private final EventRepository eventRepository;
    private final NaiveBookingsService naiveBookingsService;

    public BookingController(StockService stockService, RedisBookingService redisBookingService, EventRepository eventRepository, NaiveBookingsService naiveBookingsService) {
        this.stockService = stockService;
        this.redisBookingService = redisBookingService;
        this.eventRepository = eventRepository;
        this.naiveBookingsService = naiveBookingsService;
    }

    public record CreateEventRequest(String name, int totalSeats) {
    }

    @PostMapping("/events")
    public Event createEvent(@RequestBody CreateEventRequest req) {
        Event event = eventRepository.save(Event.builder()
                .name(req.name())
                .totalSeats(req.totalSeats())
                .availableSeats(req.totalSeats())
                .build());
        stockService.initStock(event.getId(), req.totalSeats());
        return event;
    }

    @PostMapping("/events/{eventId}/book-fast")
    public Booking bookFast(@PathVariable Long eventId, @RequestParam String userId) {
        return redisBookingService.book(eventId, userId);
    }

    @PostMapping("/events/{eventId}/book")
    public Booking book(@PathVariable Long eventId, @RequestParam String userId) {
        return naiveBookingsService.book(eventId, userId);
    }

    @ExceptionHandler(AlreadyBookedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleAlready(AlreadyBookedException e) {
        return e.getMessage();
    }
}