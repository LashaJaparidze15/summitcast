import { useEffect, useMemo } from 'react'
import { CircleMarker, MapContainer, Polyline, TileLayer, Tooltip, useMap } from 'react-leaflet'
import { formatTime, LEVEL_COLOURS } from '../format.js'

/** Splits the route into runs of consecutive points with the same risk level, so each run gets one colour. */
function segmentsByLevel(points) {
  const segments = []
  let current = null
  points.forEach((p, i) => {
    if (!current || current.level !== p.level) {
      current = { level: p.level, positions: i > 0 ? [[points[i - 1].lat, points[i - 1].lon]] : [] }
      segments.push(current)
    }
    current.positions.push([p.lat, p.lon])
  })
  return segments
}

function FitToRoute({ points }) {
  const map = useMap()
  useEffect(() => {
    map.fitBounds(points.map((p) => [p.lat, p.lon]), { padding: [24, 24] })
  }, [map, points])
  return null
}

export default function RouteMap({ points, hovered }) {
  const segments = useMemo(() => segmentsByLevel(points), [points])
  const start = points[0]
  const highest = points.reduce((a, b) => (b.elevationM > a.elevationM ? b : a))
  const hoveredPoint = hovered != null ? points[hovered] : null

  return (
    <div className="card map-card">
      <MapContainer center={[start.lat, start.lon]} zoom={13} className="map">
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
        <FitToRoute points={points} />
        {segments.map((s, i) => (
          <Polyline key={i} positions={s.positions} pathOptions={{ color: LEVEL_COLOURS[s.level], weight: 5 }} />
        ))}
        <CircleMarker center={[start.lat, start.lon]} radius={7} pathOptions={{ color: '#1f4e5f', fillOpacity: 1 }}>
          <Tooltip>Start {formatTime(start.arrival)}</Tooltip>
        </CircleMarker>
        <CircleMarker center={[highest.lat, highest.lon]} radius={7} pathOptions={{ color: '#111', fillColor: '#fff', fillOpacity: 1 }}>
          <Tooltip>High point {highest.elevationM} m, {formatTime(highest.arrival)}</Tooltip>
        </CircleMarker>
        {hoveredPoint && (
          <CircleMarker center={[hoveredPoint.lat, hoveredPoint.lon]} radius={8}
            pathOptions={{ color: '#111', fillColor: LEVEL_COLOURS[hoveredPoint.level], fillOpacity: 1 }} />
        )}
      </MapContainer>
    </div>
  )
}
