import { useState } from 'react'
import { requestPlan } from './api.js'
import { defaultStart } from './format.js'
import PlanForm from './components/PlanForm.jsx'
import Summary from './components/Summary.jsx'
import RouteMap from './components/RouteMap.jsx'
import ElevationProfile from './components/ElevationProfile.jsx'
import Timeline from './components/Timeline.jsx'
import './App.css'

export default function App() {
  const [file, setFile] = useState(null)
  const [start, setStart] = useState(defaultStart())
  const [pace, setPace] = useState(1.0)
  const [plan, setPlan] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)
  const [hovered, setHovered] = useState(null)

  async function submit() {
    setLoading(true)
    setError(null)
    try {
      setPlan(await requestPlan(file, start, pace))
      setHovered(null)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }

  async function loadSample() {
    const res = await fetch('/wicklow-white-hill-loop.gpx')
    const blob = await res.blob()
    setFile(new File([blob], 'wicklow-white-hill-loop.gpx', { type: 'application/gpx+xml' }))
  }

  return (
    <div className="app">
      <header className="topbar">
        <h1>SummitCast</h1>
        <p>Route-aware mountain weather: what the forecast says where you will be, when you will be there.</p>
      </header>

      <div className="layout">
        <aside className="sidebar">
          <PlanForm
            file={file}
            onFile={setFile}
            onSample={loadSample}
            start={start}
            onStart={setStart}
            pace={pace}
            onPace={setPace}
            loading={loading}
            onSubmit={submit}
          />
          {error && <div className="notice error">{error}</div>}
          {plan && <Summary plan={plan} />}
          <p className="disclaimer">
            SummitCast supports planning. It does not replace mountain skills, navigation or judgement.
          </p>
        </aside>

        <main className="content">
          {plan ? (
            <>
              <RouteMap points={plan.points} hovered={hovered} />
              <ElevationProfile points={plan.points} onHover={setHovered} />
              <Timeline points={plan.points} />
            </>
          ) : (
            <div className="empty">Upload a GPX route and a start time to see conditions along the way.</div>
          )}
        </main>
      </div>
    </div>
  )
}
