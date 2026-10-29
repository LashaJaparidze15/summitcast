export async function requestPlan(file, start, paceFactor) {
  const body = new FormData()
  body.append('file', file)
  body.append('start', start)
  body.append('paceFactor', paceFactor)

  let response
  try {
    response = await fetch('/api/plan', { method: 'POST', body })
  } catch {
    throw new Error('Could not reach the SummitCast server. Is the backend running?')
  }
  const data = await response.json().catch(() => ({}))
  if (!response.ok) {
    throw new Error(data.error || `Request failed (${response.status})`)
  }
  return data
}
