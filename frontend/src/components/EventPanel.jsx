import { useState } from 'react'
import { motion } from 'framer-motion'
import { createEvent } from '../api'
import { seatCounts } from '../stats'

function StatCard({ label, value, color, helper }) {
  return (
    <div className="rounded-lg bg-slate-50 border border-slate-200 p-4 shadow-xs">
      <div className="text-xs font-semibold uppercase tracking-wider text-slate-500">{label}</div>
      <div className={`mt-2 text-3xl font-bold ${color}`}>
        {typeof value === 'number' ? value.toLocaleString() : value}
      </div>
      {helper && <div className="mt-1 text-xs text-slate-500">{helper}</div>}
    </div>
  )
}

export default function EventPanel({ event, stats, onCreated }) {
  const [name, setName] = useState('Concert')
  const [seats, setSeats] = useState(100)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const { total, available, confirmed, held } = seatCounts(event, stats)

  async function handleCreate(e) {
    e?.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const res = await createEvent(name, Number(seats))
      if (res.status === 200 || res.status === 201) {
        onCreated(res.body)
      } else {
        setError(typeof res.body === 'string' ? res.body : 'Failed to create event')
      }
    } catch {
      setError('Could not reach the server. Is the Docker stack running?')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="mt-6 rounded-xl bg-white border border-slate-200 p-6 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold text-slate-900">Event Configuration</h2>
          <p className="text-sm text-slate-600">
            {event ? (
              <span>
                Active Event: <span className="font-semibold text-indigo-600">{event.name}</span>{' '}
                <span className="font-mono text-xs text-slate-500">(ID: {event.id})</span>
              </span>
            ) : (
              'Create an event to observe atomic seat holds, bookings, and simulation.'
            )}
          </p>
        </div>

        <form onSubmit={handleCreate} className="flex flex-wrap items-end gap-3">
          <label className="text-xs font-medium text-slate-700">
            Event Name
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="mt-1 block rounded bg-white px-3 py-1.5 text-sm text-slate-900 border border-slate-300 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
              required
            />
          </label>
          <label className="text-xs font-medium text-slate-700">
            Seats
            <input
              type="number"
              min="1"
              value={seats}
              onChange={(e) => setSeats(e.target.value)}
              className="mt-1 block w-24 rounded bg-white px-3 py-1.5 text-sm text-slate-900 border border-slate-300 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
              required
            />
          </label>
          <button
            type="submit"
            disabled={loading}
            className="rounded bg-indigo-600 px-4 py-1.5 text-sm font-semibold text-white hover:bg-indigo-500 disabled:opacity-50 cursor-pointer shadow-xs"
          >
            {loading ? 'Creating...' : event ? 'New Event' : 'Create Event'}
          </button>
        </form>
      </div>

      {error && <p className="mt-3 text-sm text-red-600">{error}</p>}

      {event && (
        <div className="mt-6 grid grid-cols-2 gap-4 sm:grid-cols-4">
          <StatCard
            label="Available in Redis"
            value={stats ? available : '...'}
            color="text-emerald-600"
            helper="Atomic seat stock"
          />
          <StatCard
            label="Currently Held"
            value={stats ? held : '...'}
            color="text-amber-600"
            helper="Temporary hold TTL"
          />
          <StatCard
            label="Confirmed Bookings"
            value={stats ? confirmed : '...'}
            color="text-indigo-600"
            helper="Persisted in Postgres"
          />
          <StatCard
            label="Total Capacity"
            value={stats ? total : '...'}
            color="text-slate-900"
            helper="Initial event seats"
          />
        </div>
      )}
    </section>
  )
}
