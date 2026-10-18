// Every call returns { status, servedBy, body } so the UI can show
// both the result and which backend instance handled the request.
const BASE = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '') + '/api'

async function call(path, options) {
  const res = await fetch(BASE + path, options);
  const servedBy = res.headers.get('X-Served-By');

  const text = await res.text();
  let body
  try {
    body = JSON.parse(text);
  } catch {
    body = text; // e.g. the plain-text "Sold Out" from a 409
  }
  return { status: res.status, servedBy, body }
}

export const createEvent = (name, totalSeats) =>
  call('/events', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, totalSeats }),
  })

export const getStats = (eventId) => call(`/events/${eventId}/stats`)

export const holdSeat = (eventId, userId) =>
  call(`/events/${eventId}/hold?userId=${encodeURIComponent(userId)}`, {
    method: 'POST',
  })

export const confirmSeat = (eventId, userId) =>
  call(`/events/${eventId}/confirm?userId=${encodeURIComponent(userId)}`, {
    method: 'POST',
  })
