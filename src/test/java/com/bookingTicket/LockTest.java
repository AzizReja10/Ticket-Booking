package com.bookingTicket;

import com.bookingTicket.entities.Event;
import com.bookingTicket.repository.BookingRepository;
import com.bookingTicket.repository.EventRepository;
import com.bookingTicket.service.HoldBookingService;
import com.bookingTicket.service.RedisLockService;
import com.bookingTicket.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties = "booking.hold-ttl-seconds=60")
public class LockTest {
    @Autowired
    RedisLockService lockService;
    @Autowired
    HoldBookingService holdBookingService;
    @Autowired
    StockService stockService;
    @Autowired
    EventRepository eventRepository;
    @Autowired
    BookingRepository bookingRepository;
    @Test
    void onlyOwnerCanUnlock() {
        String name = "t-" + UUID.randomUUID();

        String token = lockService.tryLock(name, Duration.ofSeconds(5));
        assertNotNull(token);
        assertNull(lockService.tryLock(name, Duration.ofSeconds(5)));   // already held

        assertFalse(lockService.unlock(name, "someone-elses-token"));   // wrong owner
        assertNull(lockService.tryLock(name, Duration.ofSeconds(5)));   // still held

        assertTrue(lockService.unlock(name, token));
        assertNotNull(lockService.tryLock(name, Duration.ofSeconds(5))); // free again
    }

    @Test
    void lockExpiresOnItsOwn() throws Exception {
        String name = "t-" + UUID.randomUUID();
        assertNotNull(lockService.tryLock(name, Duration.ofMillis(300)));
        Thread.sleep(500);
        assertNotNull(lockService.tryLock(name, Duration.ofSeconds(5)));
    }

    @Test
    void doubleClickConfirmCreatesOneBooking() throws Exception {
        Event e = eventRepository.save(Event.builder()
                .name("Test").totalSeats(5).availableSeats(5).build());
        stockService.initStock(e.getId(), 5);
        holdBookingService.hold(e.getId(), "alice");

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger ok = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    startGun.await();
                    holdBookingService.confirm(e.getId(), "alice");
                    ok.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }
        startGun.countDown();
        done.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertEquals(1, ok.get());
        assertEquals(1, bookingRepository.countByEventId(e.getId()));
    }
}
