const inr = new Intl.NumberFormat('en-IN', {
  style: 'currency',
  currency: 'INR',
  maximumFractionDigits: 0,
})

/** The API sends money as a JSON number; formatting happens only at the edge, in the UI. */
export function formatPrice(amount: number): string {
  return inr.format(amount)
}

export function formatDateTime(iso: string): string {
  return new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(iso))
}

export interface TimeLeft {
  totalMs: number
  days: number
  hours: number
  minutes: number
  seconds: number
}

export function timeLeft(endIso: string, nowMs: number): TimeLeft {
  const totalMs = Math.max(0, Date.parse(endIso) - nowMs)
  const totalSeconds = Math.floor(totalMs / 1000)
  return {
    totalMs,
    days: Math.floor(totalSeconds / 86_400),
    hours: Math.floor((totalSeconds % 86_400) / 3600),
    minutes: Math.floor((totalSeconds % 3600) / 60),
    seconds: totalSeconds % 60,
  }
}

export function formatTimeLeft(t: TimeLeft): string {
  if (t.totalMs === 0) return 'Ended'
  const pad = (n: number) => String(n).padStart(2, '0')
  if (t.days > 0) return `${t.days}d ${pad(t.hours)}h ${pad(t.minutes)}m`
  return `${pad(t.hours)}:${pad(t.minutes)}:${pad(t.seconds)}`
}
