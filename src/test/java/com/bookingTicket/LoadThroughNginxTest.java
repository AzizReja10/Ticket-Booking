package com.bookingTicket;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Semaphore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
public class LoadThroughNginxTest {
    private static final String BASE = "http://localhost:8080/api";
    private static final int SEATS = 100;
    private static final int USERS = 1000;

    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final Semaphore inFlight = new Semaphore(200);

    record Outcome(String user, int status, String servedBy) {}

    @Test
    void thousandUsersThreeInstancesHundredSeats() throws Exception {
        long eventId = createEvent("Load Test Concert", SEATS);

        List<String> users = new ArrayList<>();
        for (int i = 0; i < USERS; i++) users.add("user-" + i);

        // Phase 1: everybody tries to hold a seat at the same moment
        List<Outcome> holds = blast(eventId, users, "hold");
        summarize("HOLD PHASE", holds);
        long hung = holds.stream().filter(o -> o.status() == -1).count();
        System.out.println("Hold requests with no response: " + hung
                + " (cannot hold a seat: all " + SEATS + " went to clients that saw 200)");
        assertTrue(holds.stream().noneMatch(o -> o.status() >= 500), "5xx in hold phase");
        List<String> winners = holds.stream()
                .filter(o -> o.status() == 200)
                .map(Outcome::user)
                .toList();

        // Phase 2: winners confirm at the same moment
        List<Outcome> confirms = blast(eventId, winners, "confirm");
        List<String> retry = confirms.stream()
                .filter(o -> o.status() == -1).map(Outcome::user).toList();
        if (!retry.isEmpty()) {
            System.out.println("Retrying " + retry.size() + " unanswered confirms (idempotent, so safe)");
            List<Outcome> retried = blast(eventId, retry, "confirm");
            confirms = new ArrayList<>(confirms.stream().filter(o -> o.status() != -1).toList());
            confirms.addAll(retried);
        }
        summarize("CONFIRM PHASE", confirms);

        // Server-side truth
        String stats = get("/events/" + eventId + "/stats");
        long redisStock = extract(stats, "redisStock");
        long bookings = extract(stats, "confirmedBookings");

        System.out.println("=========== FINAL STATE ===========");
        System.out.println("Redis stock left      : " + redisStock);
        System.out.println("Bookings in Postgres  : " + bookings);
        System.out.println("===================================");

        assertEquals(SEATS, winners.size(), "Wrong number of hold winners");
        assertEquals(USERS - SEATS - hung,
                holds.stream().filter(o -> o.status() == 409).count(),
                "Everyone who got an answer, and did not win, should get 409");
        assertTrue(confirms.stream().allMatch(o -> o.status() == 200),
                "Every winner should confirm successfully");
        assertEquals(SEATS, bookings, "Overselling or lost booking in Postgres");
        assertEquals(0, redisStock, "Stock should be fully consumed");

        long instancesUsed = holds.stream().map(Outcome::servedBy).distinct().count();
        assertTrue(instancesUsed >= 2, "Load was not spread across instances");
    }

    // ---------- helpers ----------

    private List<Outcome> blast(long eventId, List<String> users, String action) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(users.size());
        CountDownLatch startGun = new CountDownLatch(1);
        List<Future<Outcome>> futures = new ArrayList<>();

        for (String user : users) {
            futures.add(pool.submit(() -> {
                startGun.await();
                inFlight.acquire();
                try {
                    HttpRequest req = HttpRequest.newBuilder()
                            .uri(URI.create(BASE + "/events/" + eventId + "/" + action + "?userId=" + user))
                            .timeout(Duration.ofSeconds(60))
                            .POST(HttpRequest.BodyPublishers.noBody())
                            .build();
                    HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
                    return new Outcome(user, res.statusCode(),
                            res.headers().firstValue("X-Served-By").orElse("?"));
                } catch (Exception e) {
                    return new Outcome(user, -1,
                            "client-error: " + e.getClass().getSimpleName() + " " + e.getMessage());
                } finally {
                    inFlight.release();
                }
            }));
        }

        startGun.countDown();

        List<Outcome> outcomes = new ArrayList<>();
        for (Future<Outcome> f : futures) outcomes.add(f.get(180, TimeUnit.SECONDS));
        pool.shutdown();
        return outcomes;
    }

    private long createEvent(String name, int seats) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/events"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"name\":\"" + name + "\",\"totalSeats\":" + seats + "}"))
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, res.statusCode(), "Could not create event: " + res.body());
        return extract(res.body(), "id");
    }

    private String get(String path) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(BASE + path)).GET().build();
        return client.send(req, HttpResponse.BodyHandlers.ofString()).body();
    }

    private long extract(String json, String field) {
        Matcher m = Pattern.compile("\"" + field + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        assertTrue(m.find(), "Field '" + field + "' not found in: " + json);
        return Long.parseLong(m.group(1));
    }

    private void summarize(String title, List<Outcome> outcomes) {
        Map<Integer, Long> byStatus = outcomes.stream()
                .collect(Collectors.groupingBy(Outcome::status, TreeMap::new, Collectors.counting()));
        Map<String, Long> byInstance = outcomes.stream()
                .collect(Collectors.groupingBy(Outcome::servedBy, TreeMap::new, Collectors.counting()));

        System.out.println("=========== " + title + " ===========");
        System.out.println("By HTTP status : " + byStatus);
        System.out.println("By instance    : " + byInstance);
    }
}
