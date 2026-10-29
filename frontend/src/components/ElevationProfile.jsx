import { useMemo } from 'react'
import { Chart as ChartJS, Filler, LinearScale, LineElement, PointElement, Tooltip } from 'chart.js'
import { Line } from 'react-chartjs-2'
import { formatTime, LEVEL_COLOURS } from '../format.js'

ChartJS.register(LinearScale, PointElement, LineElement, Filler, Tooltip)

function conditionsLine(p) {
  const c = p.conditions
  if (!c) return 'No forecast for this time'
  const gust = c.gustKmh != null ? `, gusts ${Math.round(c.gustKmh)}` : ''
  return `${c.temperatureC}°C, wind ${Math.round(c.windKmh)}${gust} km/h`
}

export default function ElevationProfile({ points, onHover }) {
  const data = useMemo(() => ({
    datasets: [{
      data: points.map((p) => ({ x: p.distanceKm, y: p.elevationM })),
      fill: true,
      backgroundColor: 'rgba(31, 78, 95, 0.12)',
      borderWidth: 3,
      pointRadius: 0,
      pointHoverRadius: 5,
      segment: {
        borderColor: (ctx) => LEVEL_COLOURS[points[ctx.p1DataIndex].level],
      },
    }],
  }), [points])

  const options = useMemo(() => ({
    responsive: true,
    maintainAspectRatio: false,
    animation: false,
    interaction: { mode: 'index', intersect: false },
    scales: {
      x: { type: 'linear', title: { display: true, text: 'Distance (km)' }, max: points[points.length - 1].distanceKm },
      y: { title: { display: true, text: 'Elevation (m)' } },
    },
    plugins: {
      tooltip: {
        callbacks: {
          title: (items) => {
            const p = points[items[0].dataIndex]
            return `${formatTime(p.arrival)} · ${p.distanceKm.toFixed(1)} km · ${p.elevationM} m`
          },
          label: (item) => conditionsLine(points[item.dataIndex]),
          afterLabel: (item) => points[item.dataIndex].hazards.map((h) => h.explanation),
        },
      },
    },
    onHover: (_event, elements) => onHover(elements.length ? elements[0].index : null),
  }), [points, onHover])

  return (
    <div className="card">
      <h2>Elevation profile</h2>
      <div className="chart">
        <Line data={data} options={options} />
      </div>
      <div className="legend">
        {['ok', 'caution', 'danger', 'unknown'].map((l) => (
          <span key={l}><i style={{ background: LEVEL_COLOURS[l] }} />{l === 'ok' ? 'no hazards' : l === 'unknown' ? 'no forecast' : l}</span>
        ))}
      </div>
    </div>
  )
}
