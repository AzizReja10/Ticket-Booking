import { useState, useEffect, useRef } from 'react'
import { motion } from 'framer-motion'
import { holdSeat, confirmSeat } from '../api'

const DEFAULT_TTL_SECONDS = 30

export default function HoldPanel({ event }) {
  const [userId, setUserId] = useState(() => `user-${Math.floor(Math.random() * 9000 + 1000)}`)
  const [status, setStatus] = useState('idle') // idle | holding | held | confirming | confirmed | expired | error
  const [message, setMessage] = useState('')
  const [servedBy, setServedBy] = useState(null)
  const [timeLeft, setTimeLeft] = useState(0)

  const timerRef = useRef(null)

  useEffect(() => {
    return () => {
      if (timerRef.current) clearInterval(timerRef.current)
    }
  }, [])

  function startCountdown(seconds) {
    if (timerRef.current) clearInterval(timerRef.current)
    setTimeLeft(seconds)
    timerRef.current = setInterval(() => {
      setTimeLeft((prev) => {
        if (prev <= 1) {
          clearInterval(timerRef.current)
          setStatus('expired')
          setMessage('Hold expired. The seat has returned to the Redis pool.')
          return 0
        }
        return prev - 1
      })
    }, 1000)
  }

  async function handleHold() {
    if (!userId.trim()) {
      setMessage('Please enter a user ID.')
      return
    }

    setStatus('holding')
    setMessage('Requesting seat hold in Redis...')
    setServedBy(null)

    try {
      const res = await holdSeat(event.id, userId.trim())
      setServedBy(res.servedBy)

      if (res.status === 200) {
        setStatus('held')
        setMessage(`Hold granted by ${res.servedBy || 'server'}! Confirm before the timer runs out.`)
        startCountdown(DEFAULT_TTL_SECONDS)
      } else if (res.status === 409) {
        setStatus('error')
        setMessage(typeof res.body === 'string' ? res.body : 'Sold out or already held by this user.')
      } else {
        setStatus('error')
        setMessage(`Hold failed with status ${res.status}`)
      }
    } catch {
      setStatus('error')
      setMessage('Network error. Is the backend running?')
    }
  }

  async function handleConfirm() {
    if (timerRef.current) clearInterval(timerRef.current)
    setStatus('confirming')
    setMessage('Confirming booking into database...')

    try {
      const res = await confirmSeat(event.id, userId.trim())
      setServedBy(res.servedBy)

      if (res.status === 200) {
        setStatus('confirmed')
        setMessage(`Booking confirmed by ${res.servedBy || 'server'}! Seat is permanently booked.`)
      } else {
        setStatus('error')
        setMessage(`Confirmation failed: ${typeof res.body === 'string' ? res.body : res.status}`)
      }
    } catch {
      setStatus('error')
      setMessage('Network error during confirmation.')
    }
  }

  function handleReset() {
    if (timerRef.current) clearInterval(timerRef.current)
    setStatus('idle')
    setMessage('')
    setServedBy(null)
    setTimeLeft(0)
    setUserId(`user-${Math.floor(Math.random() * 9000 + 1000)}`)
  }

  const isHeld = status === 'held'
  const isBusy = status === 'holding' || status === 'confirming'

  return (
    <section className="mt-8 rounded-xl bg-white border border-slate-200 p-6 shadow-sm">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 className="text-lg font-semibold text-slate-900">Single-User Hold Flow</h2>
          <p className="text-sm text-slate-600">
            Simulate an individual user holding a seat with a TTL countdown, then confirming or expiring.
          </p>
        </div>
        {servedBy && (
          <span className="rounded-full bg-indigo-50 px-3 py-1 font-mono text-xs text-indigo-700 border border-indigo-200">
            Served by: {servedBy}
          </span>
        )}
      </div>

      <div className="mt-4 flex flex-wrap items-end gap-3">
        <label className="text-xs font-medium text-slate-700">
          User ID
          <input
            type="text"
            value={userId}
            onChange={(e) => setUserId(e.target.value)}
            disabled={isHeld || isBusy}
            className="mt-1 block rounded bg-white px-3 py-2 text-sm text-slate-900 border border-slate-300 disabled:opacity-50 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500"
          />
        </label>

        {status !== 'held' && status !== 'confirmed' && (
          <button
            onClick={handleHold}
            disabled={isBusy}
            className="rounded bg-amber-600 px-4 py-2 text-sm font-semibold text-white hover:bg-amber-500 disabled:opacity-50 cursor-pointer shadow-xs"
          >
            {status === 'holding' ? 'Holding...' : 'Hold a seat'}
          </button>
        )}

        {status === 'held' && (
          <button
            onClick={handleConfirm}
            className="rounded bg-emerald-600 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-500 cursor-pointer shadow-xs"
          >
            Confirm seat
          </button>
        )}

        {(status === 'confirmed' || status === 'expired' || status === 'error') && (
          <button
            onClick={handleReset}
            className="rounded bg-slate-100 px-4 py-2 text-sm text-slate-700 hover:bg-slate-200 cursor-pointer border border-slate-300 shadow-xs"
          >
            Try another hold
          </button>
        )}
      </div>

      {isHeld && (
        <div className="mt-4">
          <div className="flex justify-between text-xs text-amber-700 font-medium">
            <span>Hold active - Complete booking:</span>
            <span>{timeLeft}s remaining</span>
          </div>
          <div className="mt-1.5 h-2 overflow-hidden rounded-full bg-slate-100 border border-slate-200">
            <motion.div
              className="h-full bg-amber-500"
              initial={{ width: '100%' }}
              animate={{ width: `${(timeLeft / DEFAULT_TTL_SECONDS) * 100}%` }}
              transition={{ duration: 1, ease: 'linear' }}
            />
          </div>
        </div>
      )}

      {message && (
        <motion.div
          initial={{ opacity: 0, y: 4 }}
          animate={{ opacity: 1, y: 0 }}
          className={`mt-4 rounded-lg p-3 text-sm ${
            status === 'confirmed'
              ? 'bg-emerald-50 text-emerald-800 border border-emerald-200'
              : status === 'held'
              ? 'bg-amber-50 text-amber-800 border border-amber-200'
              : status === 'expired'
              ? 'bg-slate-100 text-slate-700 border border-slate-200'
              : status === 'error'
              ? 'bg-rose-50 text-rose-800 border border-rose-200'
              : 'bg-slate-100 text-slate-700'
          }`}
        >
          {message}
        </motion.div>
      )}
    </section>
  )
}
