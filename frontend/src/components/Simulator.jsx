import { useEffect, useRef, useState } from 'react'
import { motion } from 'framer-motion'
import { holdSeat, confirmSeat, getStats } from '../api'

const WORKERS = 50
const MAX_USERS = 2000

const emptyTally = () => ({ granted: 0, soldOut: 0, errors: 0, done: 0, byInstance: {} })

// Runs worker(item) for every item, with up to WORKERS calls in flight.
// JavaScript is single-threaded, so `next++` is safe to share between loops.
async function runPool(items, worker, cancelled) {
  let next = 0
  async function loop() {
    while (!cancelled.current) {
      const i = next++
      if (i >= items.length) return
      await worker(items[i])
    }
  }
  await Promise.all(Array.from({ length: Math.min(WORKERS, items.length) }, loop))
}

function Tile({ label, value, color }) {
  return (
    <div className="rounded-lg bg-slate-50 border border-slate-200 p-4 shadow-xs">
      <div className="text-xs font-semibold uppercase tracking-wider text-slate-500">{label}</div>
      <div className={`mt-1 text-3xl font-bold ${color}`}>{value.toLocaleString()}</div>
    </div>
  )
}

export default function Simulator({ event, onRunningChange }) {
  const [users, setUsers] = useState('500')
  const [confirmWinners, setConfirmWinners] = useState(true)
  const [phase, setPhase] = useState('idle') // idle | holding | confirming | done
  const [total, setTotal] = useState(0)
  const [view, setView] = useState(emptyTally())
  const [summary, setSummary] = useState(null)
  const [error, setError] = useState(null)

  const tally = useRef(emptyTally())
  const cancelled = useRef(false)

  const running = phase === 'holding' || phase === 'confirming'

  // Lets the page poll faster while a sale is running
  useEffect(() => {
    onRunningChange?.(running)
    return () => onRunningChange?.(false)
  }, [running, onRunningChange])

  const snapshot = () => ({
    ...tally.current,
    byInstance: { ...tally.current.byInstance },
  })

  // Counters live in a ref and are copied into state 10 times a second.
  // Calling setState once per response would re-render the page 1,000 times.
  useEffect(() => {
    if (!running) return
    const id = setInterval(() => setView(snapshot()), 100)
    return () => clearInterval(id)
  }, [running])

  useEffect(() => () => { cancelled.current = true }, [])

  function record(res) {
    const t = tally.current
    t.done++
    const who = res.servedBy ?? 'unknown'
    t.byInstance[who] = (t.byInstance[who] ?? 0) + 1
    if (res.status === 200) t.granted++
    else if (res.status === 409) t.soldOut++
    else t.errors++
  }

  async function handleStart() {
    const count = Number(users)
    if (!Number.isInteger(count) || count < 1 || count > MAX_USERS) {
      setError(`Enter a whole number of users between 1 and ${MAX_USERS}.`)
      return
    }
    setError(null)
    setSummary(null)
    tally.current = emptyTally()
    setView(emptyTally())
    setTotal(count)
    cancelled.current = false

    try {
      const before = await getStats(event.id)
      if (before.status !== 200 || before.body.redisStock < 0) {
        setError('This event is not ready. Create it again.')
        return
      }
      const availableBefore = before.body.redisStock
      const bookingsBefore = before.body.confirmedBookings

      // A new prefix per run: each user may only hold one seat per event
      const runId = Date.now().toString(36)
      const ids = Array.from({ length: count }, (_, i) => `sim-${runId}-${i}`)
      const winners = []

      setPhase('holding')
      const t0 = performance.now()

      await runPool(ids, async (id) => {
        try {
          const res = await holdSeat(event.id, id)
          if (res.status === 200) winners.push(id)
          record(res)
        } catch {
          tally.current.done++
          tally.current.errors++
        }
      }, cancelled)

      const holdMs = performance.now() - t0
      setView(snapshot())

      if (cancelled.current) {
        setPhase('idle')
        setError('Stopped. Seats already held will return to the pool when their holds expire.')
        return
      }

      let confirmed = 0
      let confirmFailed = 0
      if (confirmWinners && winners.length > 0) {
        setPhase('confirming')
        await runPool(winners, async (id) => {
          try {
            const res = await confirmSeat(event.id, id)
            if (res.status === 200) confirmed++
            else confirmFailed++
          } catch {
            confirmFailed++
          }
        }, cancelled)
      }

      const after = await getStats(event.id)
      const t = tally.current
      setSummary({
        count,
        availableBefore,
        granted: t.granted,
        soldOut: t.soldOut,
        errors: t.errors,
        holdMs,
        confirmed,
        confirmFailed,
        confirmRan: confirmWinners && winners.length > 0,
        stockAfter: after.body.redisStock,
        bookingsAdded: after.body.confirmedBookings - bookingsBefore,
        oversold: Math.max(0, t.granted - availableBefore),
      })
      setView(snapshot())
      setPhase('done')
    } catch {
      setError('Could not reach the server. Is the Docker stack running?')
      setPhase('idle')
    }
  }

  const instances = Object.entries(view.byInstance).sort(([a], [b]) => a.localeCompare(b))
  const progress = total > 0 ? (view.done / total) * 100 : 0

  return (
    <section className="mt-8 rounded-xl bg-white border border-slate-200 p-6 shadow-sm">
      <h2 className="text-lg font-semibold text-slate-900">Flash-sale simulator</h2>
      <p className="mt-1 text-sm text-slate-600">
        Fires simulated users at this event and shows who gets a seat.
      </p>

      <div className="mt-4 flex flex-wrap items-end gap-4">
        <label className="text-xs font-medium text-slate-700">
          Users
          <input
            value={users}
            onChange={(e) => setUsers(e.target.value)}
            disabled={running}
            inputMode="numeric"
            className="mt-1 block w-28 rounded bg-white px-3 py-2 text-sm text-slate-900 border border-slate-300 disabled:opacity-50 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
          />
        </label>

        <label className="flex items-center gap-2 pb-2 text-sm text-slate-700">
          <input
            type="checkbox"
            checked={confirmWinners}
            onChange={(e) => setConfirmWinners(e.target.checked)}
            disabled={running}
            className="rounded border-slate-300 text-indigo-600 focus:ring-indigo-500"
          />
          Confirm the winners
        </label>

        {!running ? (
          <button
            onClick={handleStart}
            className="rounded bg-rose-600 px-4 py-2 text-sm font-semibold text-white hover:bg-rose-500 cursor-pointer shadow-xs"
          >
            Start the sale
          </button>
        ) : (
          <button
            onClick={() => { cancelled.current = true }}
            className="rounded bg-slate-100 px-4 py-2 text-sm text-slate-700 hover:bg-slate-200 cursor-pointer border border-slate-300 shadow-xs"
          >
            Stop
          </button>
        )}
      </div>

      {error && <p className="mt-3 text-sm text-amber-700">{error}</p>}

      {(running || phase === 'done') && (
        <div className="mt-6 space-y-5">
          <div>
            <div className="flex justify-between text-xs text-slate-600 font-medium">
              <span>
                {phase === 'confirming'
                  ? 'Confirming the winners...'
                  : phase === 'holding'
                    ? 'Users are racing for seats...'
                    : 'Finished'}
              </span>
              <span>{view.done.toLocaleString()} / {total.toLocaleString()} requests</span>
            </div>
            <div className="mt-1.5 h-2 overflow-hidden rounded-full bg-slate-100 border border-slate-200">
              <motion.div
                className="h-full bg-rose-500"
                animate={{ width: `${progress}%` }}
                transition={{ duration: 0.1, ease: 'linear' }}
              />
            </div>
          </div>

          <div className="grid grid-cols-3 gap-3">
            <Tile label="Got a seat (200)" value={view.granted} color="text-emerald-600" />
            <Tile label="Sold out (409)" value={view.soldOut} color="text-slate-900" />
            <Tile label="Errors" value={view.errors} color="text-rose-600" />
          </div>

          {instances.length > 0 && (
            <div>
              <div className="text-xs font-semibold uppercase tracking-wider text-slate-500">
                Requests handled by each instance
              </div>
              <div className="mt-2 space-y-2">
                {instances.map(([name, n]) => (
                  <div key={name} className="flex items-center gap-3 text-sm">
                    <span className="w-24 shrink-0 font-mono text-xs text-slate-700">{name}</span>
                    <div className="h-3 flex-1 overflow-hidden rounded-full bg-slate-100 border border-slate-200">
                      <motion.div
                        className="h-full bg-indigo-600"
                        animate={{ width: `${view.done > 0 ? (n / view.done) * 100 : 0}%` }}
                        transition={{ duration: 0.2 }}
                      />
                    </div>
                    <span className="w-12 text-right text-xs font-mono text-slate-600">{n.toLocaleString()}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {summary && (
        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          className="mt-6 rounded-lg border border-slate-200 bg-slate-50 p-4 text-sm shadow-xs"
        >
          <div className={`text-base font-semibold ${summary.oversold === 0 ? 'text-emerald-700' : 'text-red-600'}`}>
            {summary.oversold === 0
              ? `${summary.granted.toLocaleString()} seats granted out of ${summary.availableBefore.toLocaleString()} available. Oversold: 0`
              : `Oversold by ${summary.oversold.toLocaleString()}`}
          </div>
          <ul className="mt-3 space-y-1 text-slate-700">
            <li>{summary.count.toLocaleString()} users in {(summary.holdMs / 1000).toFixed(1)} s</li>
            <li>{summary.soldOut.toLocaleString()} were told sold out</li>
            {summary.confirmRan && (
              <li>
                {summary.confirmed.toLocaleString()} of {summary.granted.toLocaleString()} winners confirmed
                {summary.confirmFailed > 0 && `, ${summary.confirmFailed} failed`}
              </li>
            )}
            <li>Server check: {summary.stockAfter.toLocaleString()} seats left in Redis, {summary.bookingsAdded.toLocaleString()} new bookings in Postgres</li>
          </ul>
          {summary.errors > 0 && (
            <p className="mt-3 text-amber-700 font-medium">
              {summary.errors} requests got no usable answer, so the client count may be
              lower than what the server granted. The server numbers are the ones to trust.
            </p>
          )}
          {!summary.confirmRan && summary.granted > 0 && (
            <p className="mt-3 text-slate-600">
              These seats are held, not booked. They return to the pool when the holds expire.
            </p>
          )}
        </motion.div>
      )}
    </section>
  )
}
