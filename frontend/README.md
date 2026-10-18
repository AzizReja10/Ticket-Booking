# Flash Sale UI

A React frontend (Vite, Tailwind CSS, Framer Motion) to watch the Flash Sale ticket booking system work in real-time.

## Features

- **Event Panel**: Create an event and monitor live seat stats (Redis stock, active holds, confirmed bookings) polled from `/events/{id}/stats`.
- **Seat Grid**: Animated visualization of seat states (Free, Held, Confirmed) using dynamic pooling (250ms during bursts, 1000ms idle).
- **Single-User Hold Flow**: Test single-user hold lifecycle with TTL countdown timer, instance identification (`X-Served-By`), and confirm / expire transitions.
- **Flash-Sale Simulator**: Fire simulated concurrent users (worker pool) at the backend, streaming 200 vs 409 responses live, visualizing load balancer distribution across three instances, and verifying against server numbers (zero overselling check).

## Quick Start

```bash
# If running standalone in flashsale-ui/
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173). The Docker stack (Spring backend + Redis + Postgres + Nginx) should be running; Vite's dev server proxies `/api` to Nginx on port `8080`.

> **Note**: The simulator sends requests from the browser, which limits concurrent connections per host (HTTP/1.1), so it behaves as a fast burst rather than 1,000 truly simultaneous requests. The Java load test remains the proof of the simultaneous spike.
