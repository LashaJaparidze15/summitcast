export const LEVEL_COLOURS = {
  ok: '#2e7d32',
  caution: '#d97706',
  danger: '#dc2626',
  unknown: '#9ca3af',
}

export const LEVEL_LABELS = {
  ok: 'No hazards',
  caution: 'Caution',
  danger: 'Danger',
  unknown: 'No forecast',
}

export function formatTime(iso) {
  return new Date(iso).toLocaleTimeString('en-IE', {
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'Europe/Dublin',
  })
}

export function formatDuration(minutes) {
  const h = Math.floor(minutes / 60)
  const m = Math.round(minutes % 60)
  return `${h}h ${String(m).padStart(2, '0')}m`
}

/** Default start: tomorrow at 09:00, in the format a datetime-local input expects. */
export function defaultStart() {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T09:00`
}
