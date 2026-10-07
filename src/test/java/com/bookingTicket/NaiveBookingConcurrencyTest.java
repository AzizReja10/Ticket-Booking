package com.bookingTicket;

import com.bookingTicket.entities.Event;
import com.bookingTicket.exceptions.SoldOutException;
import com.bookingTicket.repository.BookingRepository;
import com.bookingTicket.repository.EventRepository;
import com.bookingTicket.service.NaiveBookingsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
@SpringBootTest
class NaiveBookingConcurrencyTest {

    @Autowired NaiveBookingsService bookingService;
    @Autowired EventRepository eventRepository;
    @Autowired BookingRepository bookingRepository;

    @Test
    void thousandUsersFightOverHundredSeats() throws Exception {
        int seats = 100;
        int users = 1000;

        Event event = eventRepository.save(Event.builder()
                .name("Concert").totalSeats(seats).availableSeats(seats).build());

        ExecutorService pool = Executors.newFixedThreadPool(users);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(users);

        AtomicInteger success = new AtomicInteger();
        AtomicInteger soldOut = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();

        for (int i = 0; i < users; i++) {
            final String userId = "user-" + i;
            pool.submit(() -> {
                try {
                    startGun.await();
                    bookingService.book(event.getId(), userId);
                    success.incrementAndGet();
                } catch (SoldOutException e) {
                    soldOut.incrementAndGet();
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        startGun.countDown();          // release all threads at once
        done.await(60, TimeUnit.SECONDS);
        pool.shutdown();

        long bookingsInDb = bookingRepository.countByEventId(event.getId());
        int seatsLeft = eventRepository.findById(event.getId()).orElseThrow().getAvailableSeats();

        System.out.println("=========== RESULT ===========");
        System.out.println("Successful bookings (threads): " + success.get());
        System.out.println("Sold out responses           : " + soldOut.get());
        System.out.println("Other errors                 : " + errors.get());
        System.out.println("Bookings rows in DB          : " + bookingsInDb);
        System.out.println("availableSeats in DB         : " + seatsLeft);
        System.out.println("==============================");

        // The invariant we care about. It SHOULD fail right now.
        assertEquals(seats, bookingsInDb, "Overselling detected!");
    }
}
