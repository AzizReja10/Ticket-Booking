// Turns the /stats response into the numbers the UI shows.
// "Held" is derived: a held seat has left the Redis stock but is not yet in Postgres.
export function seatCounts(event, stats) {
  const total = event?.totalSeats ?? 0
  const available = stats?.body?.redisStock ?? total
  const confirmed = stats?.body?.confirmedBookings ?? 0
  const held = Math.max(0, total - available - confirmed)
  return { total, available, confirmed, held }
}
