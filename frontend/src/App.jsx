import { useState } from 'react'
import EventPanel from './components/EventPanel'
import SeatGrid from './components/SeatGrid'
import HoldPanel from './components/HoldPanel'
import Simulator from './components/Simulator'
import { getStats } from './api'
import { usePolling } from './hooks/usePolling'

export default function App() {
  const [event, setEvent] = useState(null)
  const [saleRunning, setSaleRunning] = useState(false)

  // One poll feeds the stat cards and the seat grid. Faster while a sale runs.
  const stats = usePolling(
    () => getStats(event.id),
    saleRunning ? 250 : 1000,
    event ? event.id : null,
  )

  return (
    <div className="min-h-screen bg-white text-slate-900">
      <div className="mx-auto max-w-3xl p-8">
        <h1 className="text-3xl font-bold tracking-tight text-slate-900">Flash Sale</h1>
        <p className="mt-1 text-slate-600">
          Many users, few seats, three app instances. Seat decisions are made
          atomically in Redis.
        </p>

        <EventPanel event={event} stats={stats} onCreated={setEvent} />
        {event && <SeatGrid event={event} stats={stats} />}
        {event && <HoldPanel key={event.id} event={event} />}
        {event && (
          <Simulator key={event.id} event={event} onRunningChange={setSaleRunning} />
        )}
      </div>
    </div>
  )
}
