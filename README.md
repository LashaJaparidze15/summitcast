# SummitCast

Route-aware mountain weather planner. Give it a hillwalking route (GPX) and a start time, and it
estimates when you will reach each point of the route, then shows the Met Éireann forecast for that
place at that time and flags sections where conditions may be unsafe.

> SummitCast supports planning. It does not replace mountain skills, navigation or judgement.

## Iteration 1 (v0.1.0)

What works in this release:

- **GPX upload**: tracks or routes, parsed with [jpx](https://github.com/jenetics/jpx). Missing
  elevations between known points are interpolated. Files with no elevation at all are rejected
  with a clear message (terrain model lookup is planned).
- **Timing model**: Naismith's rule with Langmuir's descent corrections and an adjustable pace factor.
- **Forecast along the route**: Met Éireann point forecasts sampled about every 1 km along the
  route, interpolated in time to each point's estimated arrival.
- **Data-driven hazard rules**: wind, gusts, freezing temperatures and heavy rain, defined in
  [`hazard-rules.json`](backend/src/main/resources/hazard-rules.json). Every warning shows the
  forecast value, the threshold and the rule that produced it.
- **Map, elevation profile and timeline**, coloured by risk.
- **Failure handling**: forecast outages and start times beyond the forecast range still return the
  timings, with a clear message. Forecasts are cached in memory for an hour.
- **37 automated tests** (JUnit 5) covering the timing model, GPX parsing, forecast parsing,
  interpolation and hazard rules.

## Architecture

```
frontend/  React 19 + Vite, Leaflet (map), Chart.js (profile)
   │  POST /api/plan  (multipart: GPX file, start, paceFactor)
   ▼
backend/   Java 21, Spring Boot 4
   route/      GPX parsing (jpx), elevation gap filling
   timing/     Naismith + Langmuir timing model
   forecast/   Met Éireann client (HTTP + cache), XML parser, time interpolation
   hazard/     JSON rule loading and evaluation, risk levels
   plan/       Orchestration and REST API
```

Pipeline for one request:

1. Parse the GPX into points (lat, lon, elevation).
2. Walk the route segment by segment, accumulating distance and estimated time.
3. Pick forecast sample points about 1 km apart and fetch each one from Met Éireann
   (coordinates rounded to ~1 km so nearby samples share one cached request).
4. For every route point: take the nearest sample, interpolate it to the arrival time,
   evaluate the hazard rules.

## Timing model

Per segment between consecutive points:

| Rule | Value |
|---|---|
| Horizontal pace (Naismith) | 5 km/h |
| Ascent (Naismith) | + 1 hour per 600 m climbed |
| Gentle descent, 5° to 12° (Langmuir) | − 10 min per 300 m descended |
| Steep descent, over 12° (Langmuir) | + 10 min per 300 m descended |
| Descent under 5° | no correction |

The total is multiplied by the walker's pace factor (1.0 = Naismith pace).

## Running locally

Requirements: Java 21+, Node 20+. No API keys needed.

```bash
# Backend (http://localhost:8080)
cd backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run

# Frontend (http://localhost:5173), in a second terminal
cd frontend
npm install
npm run dev
```

Open http://localhost:5173, click **Use sample route** (or choose a GPX file), pick a start time
within the next few days, and click **Plan walk**.

### Tests

```bash
cd backend
./mvnw test
```

## Sample data

`samples/wicklow-white-hill-loop.gpx` is an 18.2 km loop in the Wicklow Mountains with 695 m of
ascent. It starts near Ballinastoe Woods, climbs to White Hill (629 m) on the Wicklow Way, then
loops south towards Lough Dan and back. It was planned in [gpx.studio](https://gpx.studio), which snaps to
mapped paths and adds elevation from a terrain model. It is a planned route, not a recording, so it
has no timestamps. It is for testing only, not for navigation.

The backend tests use a separate hand-sketched Carrauntoohil route in `backend/src/test/resources`.

## Known limitations (planned for later iterations)

- Forecast temperature and wind are for the forecast model's terrain height, not the walker's. On
  the summit of Carrauntoohil (1039 m) the model terrain is around 700 m, so the summit is
  forecast too warm and too calm. The error can also go the other way: on the Wicklow sample the
  model ground is about 100 m too high at the start and about 90 m too low further round the
  loop. The timeline shows the model height next to the real one.
  Altitude adjustment (lapse rate, wind with height) and wind chill come next.
- No elevation lookup for GPX files without elevation (Copernicus GLO-30 DEM planned).
- No walking-after-sunset rule yet.
- Elevation noise in recorded tracks inflates total ascent; smoothing is planned, along with
  evaluating the timing model against real timestamped tracks.

## Data and attribution

- Weather: [Met Éireann](https://www.met.ie/) open data, licensed under
  [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).
- Map tiles: © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors.
