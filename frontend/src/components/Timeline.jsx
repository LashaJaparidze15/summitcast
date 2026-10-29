import { formatTime, LEVEL_COLOURS } from '../format.js'

const STEP_MINUTES = 20

/** One row every 20 minutes of walking, plus the high point and the finish. */
function timelineRows(points) {
  const startMs = new Date(points[0].arrival).getTime()
  const highest = points.reduce((a, b) => (b.elevationM > a.elevationM ? b : a))
  const rows = []
  let next = 0
  for (const p of points) {
    const elapsed = (new Date(p.arrival).getTime() - startMs) / 60000
    if (elapsed >= next || p === highest) {
      rows.push({ ...p, isHigh: p === highest })
      if (elapsed >= next) next = Math.floor(elapsed / STEP_MINUTES) * STEP_MINUTES + STEP_MINUTES
    }
  }
  const last = points[points.length - 1]
  if (rows[rows.length - 1] !== last) rows.push(last)
  return rows
}

const fmt = (v, digits = 0) => (v == null ? '–' : v.toFixed(digits))

export default function Timeline({ points }) {
  const rows = timelineRows(points)
  return (
    <div className="card">
      <h2>Timeline</h2>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Time</th><th>km</th><th>Elevation</th><th>Temp</th><th>Wind</th><th>Gusts</th><th>Rain</th>
              <th title="Height of the forecast model's terrain at this spot">Model height</th><th>Hazards</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((p, i) => (
              <tr key={i}>
                <td><span className="dot" style={{ background: LEVEL_COLOURS[p.level] }} />{formatTime(p.arrival)}</td>
                <td>{p.distanceKm.toFixed(1)}</td>
                <td>{p.elevationM} m{p.isHigh ? ' ▲' : ''}</td>
                <td>{fmt(p.conditions?.temperatureC, 1)}°C</td>
                <td>{fmt(p.conditions?.windKmh)} km/h</td>
                <td>{fmt(p.conditions?.gustKmh)} km/h</td>
                <td>{fmt(p.conditions?.precipitationMmPerHour, 1)} mm/h</td>
                <td>{p.conditions ? `${p.conditions.modelAltitudeM} m` : '–'}</td>
                <td className="hazards">
                  {p.hazards.length === 0 ? <span className="muted">{p.conditions ? 'none' : 'no forecast'}</span>
                    : p.hazards.map((h) => <div key={h.ruleId}>{h.explanation}</div>)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
