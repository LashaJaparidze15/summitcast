export default function PlanForm({ file, onFile, onSample, start, onStart, pace, onPace, loading, onSubmit }) {
  function handleSubmit(e) {
    e.preventDefault()
    onSubmit()
  }

  return (
    <form className="card form" onSubmit={handleSubmit}>
      <label>
        Route (GPX)
        <input type="file" accept=".gpx,application/gpx+xml" onChange={(e) => onFile(e.target.files[0] ?? null)} />
      </label>
      <div className="file-row">
        <span className="muted">{file ? file.name : 'No file chosen'}</span>
        <button type="button" className="link" onClick={onSample}>Use sample route</button>
      </div>

      <label>
        Start (Irish time)
        <input type="datetime-local" value={start} onChange={(e) => onStart(e.target.value)} required />
      </label>

      <label>
        Pace factor: <strong>{pace.toFixed(2)}</strong>
        <input type="range" min="0.5" max="2.5" step="0.05" value={pace} onChange={(e) => onPace(Number(e.target.value))} />
        <span className="muted small">1.00 = Naismith pace. 1.25 = 25% slower.</span>
      </label>

      <button type="submit" disabled={!file || !start || loading}>
        {loading ? 'Planning…' : 'Plan walk'}
      </button>
    </form>
  )
}
