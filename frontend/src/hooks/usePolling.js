import { useEffect, useRef, useState } from 'react'

// Calls fn() now and then every intervalMs. Restarts when `key` or the interval changes.
// A null key means "don't poll". Data is cleared only when `key` changes.
export function usePolling(fn, intervalMs, key) {
  const [data, setData] = useState(null)
  const fnRef = useRef(fn)
  fnRef.current = fn

  useEffect(() => {
    setData(null)
  }, [key])

  useEffect(() => {
    if (key == null) return

    let cancelled = false
    async function tick() {
      try {
        const result = await fnRef.current()
        if (!cancelled) setData(result)
      } catch {
        // a failed poll is skipped; the next tick will try again
      }
    }

    tick()
    const id = setInterval(tick, intervalMs)
    return () => {
      cancelled = true
      clearInterval(id)
    }
  }, [key, intervalMs])

  return data
}
