# Design Brief — Logística G5

Approved by the user on 2026-10-31 (Phase 0 and 1). Source: `odd/tasks/frontend-from-scratch.md`.
Mode: **Standard**.

## 1. The product in one breath

| Field | Answer |
|---|---|
| What it is | A web app on top of the Group 5 logistics REST API: package registration, route assignment, delivery attempts, tracking by code and performance reports. |
| Who it is for | Three roles. **cliente** (phone and desktop, occasional), **repartidor** (phone, in the field, one hand, daylight), **despachador** (desktop, all day, dense tables and forms). |
| The job they hire it for | When I am waiting for or handling a package, I want to see exactly where it is and what happened to it, so I can plan around it or act on it. |
| Business goal | Course lab: every role flow works against the real API, is usable and accessible. Speed of comprehension over persuasion. |
| Market & audience locale | Chile. `es-CL`, Latin script, neutral professional Spanish, dates `dd-mm-aaaa`, decimal comma, time zone America/Santiago. Phones are the main device for two of three roles. |
| Primary language | `es-CL`. No language switcher (not demonstrably international). |
| Platform(s) | Web app (React 18 + Vite + React Router). |
| Expression register | **R2 Composed** because the audience is making an operational decision (where is it / what do I do next), the category norm (courier tracking, dispatch consoles) is utilitarian and the asset budget is zero photographs. One bold move (the route line) instead of ornament. |
| Interaction depth | **I3 Demonstrative** — signature interaction: enter a tracking code → the route line draws itself and the events appear in order → the same view carries the code into the copy button and the deep link `/rastreo/:codigo`. |
| Design language | Decided in `DESIGN.md` from the measured market line. |
| Mode | Standard |
| Surface type | Product UI (Operate mode) with a public entry. |
| Scope | New product, design system included. |

## 2. The memorable thing

"See where your package is at a glance." The tracking timeline is the visual anchor of the whole product.

## 3. Constraints

- **Design system:** none exists, so we build one. DTCG tokens compiled to CSS custom properties, light and dark.
- **Visual assets:** none. No photography; the anchor is a diagram (the route line) drawn in CSS/SVG.
- **Brand assets fixed:** none. Name in use: "Logística G5".
- **Tech stack:** React 18.3.1, Vite 6, React Router 6, `gsap` 3.15.0, `@gsap/react` 2.1.2, `keycloak-js` 26.2.5 (code flow, PKCE S256, client `logistica-web`). Plain CSS with tokens; no UI kit, no Tailwind.
- **Accessibility target:** WCAG 2.2 AA floor at every register.
- **Localization:** `es-CL` only; Latin script with accents (ñ, á, é, í, ó, ú).
- **Performance floor:** mid-range Android on mobile data; small bundle, no external scripts (fonts via a Google Fonts stylesheet only).
- **Legal:** none beyond ordinary personal data in addresses (shown only to authorised users).
- **Fidelity:** production code for the lab.

## 4. Users

| Proto-persona | Context of use | Top 3 tasks | Top frustration today | What "good" feels like |
|---|---|---|---|---|
| Cliente | Phone on the couch or desktop at work | Track a code, see my shipments, fix a destination | Not knowing if a failed attempt happened or why | One look and I know the state and the next step |
| Repartidor | Phone, one hand, in daylight, between stops | See today's deliveries, mark delivered, register a failed attempt | Small targets and long forms while standing | Big targets, two taps per result |
| Despachador | Desktop, all day | Assign routes, find a package, check alerts below 85% | Jumping between tables to get the full picture | Everything about a package or route on one screen |

## 5. Competitive & reference landscape

Measured in Phase 2; see the market fingerprint and references in `DESIGN.md`.

**Category conventions we respect (table stakes):** a code field as the first thing on the tracking page, a status badge, a chronological event list, dates with time, a clear "not found" state.
**Conventions we deliberately break:** the generic horizontal four-step progress bar (it hides failed attempts and returns); replaced by a vertical route line that can show a retry or a return as its own node. Reason: the brief's memorable thing is the whole story at a glance, including when it went wrong.

## 6. Brand attributes → visual consequences

| Attribute | Anti-attribute | Visual consequence |
|---|---|---|
| Legible at a glance | Decorative | One route line as the anchor; state is text + icon + shape, never colour alone; large status word. |
| Exact | Vague | Monospaced tabular codes and timestamps; every date `dd-mm-aaaa hh:mm`; hairline rules, no shadows on content. |
| Calm and competent | Loud, gimmicky | Single cold teal hue, low-chroma neutrals tinted toward it; motion only on the tracking reveal and short state changes. |

## 7. Success criteria

- `npm run build` passes; every text role passes AA in light and dark (`contrast.py --tokens`).
- `slop_lint.py` grade B or better.
- Phone width (375 px): no horizontal scroll on any screen.
- A cliente reaches their package timeline in two actions from the landing.
- Login works for the five Keycloak test users and each lands on the role home.

## 8. Open questions & assumptions

| # | Question | Assumption if unanswered | Risk if wrong | Owner |
|---|---|---|---|---|
| 1 | Anonymous visitors: the API needs a token | Landing asks for login and resumes the code after login | Visitors expect public tracking; documented limitation of the API | Backend owner |
| 2 | Timestamps carry no zone (`LocalDateTime`) | Treat them as America/Santiago wall time and print as is | Off by hours if the server runs in UTC | Backend owner |
| 3 | Logo / brand name | "Logística G5" wordmark set in type | Replace when a brand exists | User |
