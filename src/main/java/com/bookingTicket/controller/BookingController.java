package com.bookingTicket.controller;

import com.bookingTicket.entities.Booking;
import com.bookingTicket.entities.Event;
import com.bookingTicket.exceptions.AlreadyBookedException;
import com.bookingTicket.exceptions.HoldExpiredException;
import com.bookingTicket.exceptions.SoldOutException;
import com.bookingTicket.repository.BookingRepository;
import com.bookingTicket.repository.EventRepository;
import com.bookingTicket.service.HoldBookingService;
import com.bookingTicket.service.NaiveBookingsService;
import com.bookingTicket.service.RedisBookingService;
import com.bookingTicket.service.StockService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BookingController {
    private final BookingRepository bookingRepository;
    private final StockService stockService;
    private final RedisBookingService redisBookingService;
    private final EventRepository eventRepository;
    private final NaiveBookingsService naiveBookingsService;
    private final HoldBookingService holdBookingService;
    public BookingController(BookingRepository bookingRepository, StockService stockService, RedisBookingService redisBookingService, EventRepository eventRepository, NaiveBookingsService naiveBookingsService, HoldBookingService holdBookingService) {
        this.bookingRepository = bookingRepository;
        this.stockService = stockService;
        this.redisBookingService = redisBookingService;
        this.eventRepository = eventRepository;
        this.naiveBookingsService = naiveBookingsService;
        this.holdBookingService = holdBookingService;
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
    public record HoldResponse(long expiresAtEpochMs){}
    @PostMapping("/events/{eventId}/hold")
    public HoldResponse hold(@PathVariable Long eventId,@RequestParam String userId)
    {
        return new HoldResponse(holdBookingService.hold(eventId,userId));
    }
    @PostMapping("/events/{eventId}/confirm")
    public Booking confirm(@PathVariable Long eventId,@RequestParam String userId)
    {
        return holdBookingService.confirm(eventId,userId);
    }
    public record StatsResponse(long redisStock,long confirmedBookings)
    {}
    @GetMapping("/events/{eventId}/stats")
    public StatsResponse stats(@PathVariable Long eventId)
    {
        return new StatsResponse(
                stockService.getStock(eventId),
                bookingRepository.countByEventId(eventId)
        );
    }
}