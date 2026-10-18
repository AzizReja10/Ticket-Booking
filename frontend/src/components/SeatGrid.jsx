import { seatCounts } from '../stats'

const MAX_CELLS = 200

export default function SeatGrid({ event, stats }) {
  const { total, confirmed, held } = seatCounts(event, stats)
  if (total < 1) return null

  // Above 200 seats, one square stands for several seats so the grid stays readable
  const unit = Math.ceil(total / MAX_CELLS)
  const cells = Math.ceil(total / unit)

  const stateOf = (i) => {
    const end = Math.min((i + 1) * unit, total)
    if (end <= confirmed) return 'confirmed'
    if (end <= confirmed + held) return 'held'
    return 'free'
  }

  const color = {
    free: 'bg-slate-200 border border-slate-300/70',
    held: 'bg-amber-400 border border-amber-500/50',
    confirmed: 'bg-emerald-500 border border-emerald-600/50',
  }

  return (
    <section className="mt-8 rounded-xl bg-white border border-slate-200 p-6 shadow-sm">
      <div
        className="grid gap-1"
        style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(18px, 1fr))' }}
      >
        {Array.from({ length: cells }, (_, i) => (
          <div
            key={i}
            className={`aspect-square rounded-xs transition-colors duration-300 ${color[stateOf(i)]}`}
          />
        ))}
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-x-5 gap-y-1 text-xs text-slate-600">
        <span className="flex items-center gap-1.5 font-medium">
          <i className="inline-block h-3.5 w-3.5 rounded-xs bg-slate-200 border border-slate-300" /> Free
        </span>
        <span className="flex items-center gap-1.5 font-medium">
          <i className="inline-block h-3.5 w-3.5 rounded-xs bg-amber-400 border border-amber-500" /> Held
        </span>
        <span className="flex items-center gap-1.5 font-medium">
          <i className="inline-block h-3.5 w-3.5 rounded-xs bg-emerald-500 border border-emerald-600" /> Confirmed
        </span>
        <span className="text-slate-400">
          {unit === 1 ? 'One square per seat.' : `One square is about ${unit} seats.`}{' '}
          The backend tracks counts, not seat numbers, so squares fill in order.
        </span>
      </div>
    </section>
  )
}
