import { formatDuration, formatTime, LEVEL_COLOURS, LEVEL_LABELS } from '../format.js'

export default function Summary({ plan }) {
  const { summary, forecast } = plan
  const highest = plan.points.reduce((a, b) => (b.elevationM > a.elevationM ? b : a))

  return (
    <div className="card">
      <div className="overall" style={{ background: LEVEL_COLOURS[summary.overallLevel] }}>
        {LEVEL_LABELS[summary.overallLevel]}
      </div>
      <dl className="stats">
        <dt>Distance</dt><dd>{summary.distanceKm} km</dd>
        <dt>Ascent / descent</dt><dd>{summary.ascentM} m / {summary.descentM} m</dd>
        <dt>Walking time</dt><dd>{formatDuration(summary.durationMinutes)}</dd>
        <dt>Start → finish</dt><dd>{formatTime(summary.start)} → {formatTime(summary.finish)}</dd>
        <dt>High point</dt><dd>{highest.elevationM} m at {formatTime(highest.arrival)}</dd>
      </dl>
      <div className={`notice ${forecast.available ? 'info' : 'error'}`}>
        {forecast.message}
        {forecast.available && (
          <span className="muted small"> {forecast.locationsQueried} forecast locations. Source: {forecast.source}.</span>
        )}
      </div>
    </div>
  )
}
