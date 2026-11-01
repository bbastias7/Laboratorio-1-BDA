# Logística G5 Design System — DESIGN.md

> Single source of truth for how the Group 5 web app looks, feels and behaves. Load this before designing a screen.
> Raw values live in `frontend/tokens/*.json` (DTCG); this document explains why. Code identifiers and this file are
> in English; UI copy is in Spanish (es-CL).

## 0. Status

| Field | Value |
|---|---|
| Version | 0.2.0 |
| Owner | Group 5 (lab project) |
| Last reviewed | 2026-10-31 (full review, section 11) |
| Applies to | web app (React + Vite) |
| Market & audience | Chile; `es-CL`, Latin script; `dd-mm-aaaa`, decimal comma, America/Santiago; phone-first for repartidor and cliente, desktop for despachador |
| Primary language | `es-CL` (`<html lang="es-CL">`). The audience is local, there is no switcher. |
| Surface mode | Operate (with a public entry) |
| Expression register | **R2 Composed** because the audience makes an operational decision, the category (courier tracking, dispatch consoles) is utilitarian, and there is no asset budget. One bold move: the route line. |
| Interaction depth | **I3 Demonstrative** because the top job (where is my package) is answered by a result the visitor produces from a code. |
| Design language | `light;low;grotesk;medium;square;none;left;functional;solid` — from the market line, departing on imagery, density, layout and motion (below) |
| Mode | Standard |
| Token source | `frontend/tokens/primitives.json`, `semantic.json`, `semantic.dark.json`, `components.json` |
| Build | `python3 ~/.claude/skills/no-slop-design/scripts/build_tokens.py frontend/tokens/*.json --out frontend/build/ --format css,flat-json`, then copy `build/tokens.css` to `src/styles/tokens.css` (the app imports that copy) |

## 1. Foundations of intent

**The memorable thing:** see where your package is at a glance.

| We are | We are not | Visual consequence |
|---|---|---|
| Legible at a glance | Decorative | One route line as the anchor; state is word + icon + node shape, never colour alone |
| Exact | Vague | Mono tabular codes and timestamps, hairline rules, no shadow on content |
| Calm and competent | Loud | One cold teal, tinted neutrals, motion only where it explains something |

**Direction: "Hoja de Ruta"** (waybill) — Swiss-grid discipline of a printed waybill, in a cold teal world, at the density of an
operations console, with a vertical route line as the only graphic.

**Design language:** light;low;grotesk;medium;square;none;left;functional;solid
Market: light;achromatic;humanist;airy;square;photo;asymmetric;none;solid — from 3 pages: https://www.ups.com/track?loc=es_CL https://www.fedex.com/es-cl/tracking.html https://www.correos.cl/seguimiento-en-linea (bot wall, read by eye); plus by eye / measured separately: https://www.starken.cl/seguimiento (light;low;grotesk;airy;soft;photo;asymmetric;choreographed;solid, green brand, photo carousel, search pill) and https://onfleet.com (mixed;saturated;grotesk;medium;square;photo;left;none;hairline, dispatcher console). chilexpress.cl, blue.cl and dhl.com blocked or timed out in headless Chrome (HTTP error / timeout) and were not measurable.
Splits: type (humanist / serif / grotesk), density (airy ×2, medium), imagery (photo ×2, none), layout (asymmetric ×2, centered), motion (none ×2, scene), and across the two Chilean/dispatcher pages surface, chroma, radius and controls. The market is unanimous on: light surface, low/achromatic colour, square-ish controls, solid primary button. We keep those.
Departs on: imagery (none, not photo) — the product has no photography, the anchor is a diagram, and an operations tool for repartidores must not pay for imagery on mobile data · density (medium, not airy) — Operate mode, the despachador reads tables · layout (left, not asymmetric) — a form-and-list product reads left to right, the first viewport is a left-aligned heading with the code field · motion (functional, not none) — one reveal on the timeline, bought by the brief's signature interaction. Chroma: low (market low/achromatic) with a teal hue that none of the measured pages owns (Starken green, FedEx/UPS neutral, Onfleet saturated multicolour).
Market opening: h-centre;panel-none;img-left (market line) · Opening: heading left + code field in a panel (`h-left;panel-under;img-none`). It differs because the first viewport must hold the tracking input (the product's top job) and the category pages that have one all place it under or beside the heading, not in a centred stack. Reason: Operate surface, no image to centre around.
Nearest calibration point: the neutral component library / GOV.UK-like service, with a console-density variation. Measured after the build: recorded in section 11.

**References** (≥ 5; ≥ 2 local; ≥ 30% non-UI):

| # | Source | Type | Taken | Serves |
|---|---|---|---|---|
| R1 | https://www.starken.cl/seguimiento | local courier (UI) | The tracking field sits in the first viewport as a single input with a search icon; keep it, drop the photo carousel | signature interaction placement |
| R2 | https://www.ups.com/track?loc=es_CL | global courier (UI) | The status headline above the event list, events in reverse-chronological reading blocks; keep "status first, history second" | package header |
| R3 | https://www.fedex.com/es-cl/tracking.html | global courier (UI) | Code + copy affordance, "not found" copy that says what to check | empty and error states |
| R4 | https://onfleet.com | global dispatch console (UI) | Hairline controls and dense list + detail for the dispatcher; no cards around lists | despachador density |
| R5 | Metro de Santiago line diagram (station signage, local, non-UI) | wayfinding | Vertical line, round node per stop, the current stop as a ring, the line already travelled drawn solid and the rest dashed | route line anchor |
| R6 | Chilean "boleta"/waybill (guía de despacho) paper layout, local, non-UI | printed document | Monospaced code in a ruled box, label above value, hairline rules | code typography and header |
| R7 | Japanese railway timetable boards (non-UI, global) | signage | Time in a fixed mono column left of the node, text right | timeline row grid |

**Alternative considered — "Torre de Control" (R3 Expressive)**: a dark full-bleed map-like console with a saturated amber signal colour,
large poster-scale package codes and animated route traces. More memorable, but it needs map data the API does not expose, costs battery
and legibility in daylight for repartidores, and pushes density below what despachador tables need. Not chosen; recorded so ambition can be
traded without redoing research.

**Imagery art direction:** subject — the route itself · people none · light n/a · treatment flat line and node in `route.*` tokens · crop n/a ·
ratios n/a · banned: stock photos of trucks and boxes, map screenshots, truck/package emoji, illustrated couriers.
Anchor type: diagram. Photographs needed: no — the anchor is the route-line diagram drawn in CSS and inline SVG from state data.

**Register techniques used:** poster-scale mono code (display ratio ≥ 2.5× body) · one saturated field (the action button) · route-line diagram · hairline rules · short GSAP reveal. All on the R2 row; no gradients, no glass, no grain.

**Signature interaction:** the visitor types or pastes a tracking code (or arrives by `/rastreo/:codigo`) → the package header appears and the route line
draws top to bottom with the events staggering in, the current state ringed and a failed attempt or return rendered as its own node with icon and text →
the result carries into a copy-code button, a shareable URL and, for the cliente, the destination-change action in F4. Declared as
`data-nsd-interaction="tracking-code"`. Without JavaScript: the app needs JavaScript to log in at all, so there is no static equivalent;
a `<noscript>` message states that. Result form: **diagram** — because the answer is a sequence in time with a current position.

**Anonymous visitors.** The API requires a token even for tracking, so the landing shows the code field and a button "Ingresar para rastrear".
Submitting a code while logged out stores the code in the Keycloak `redirectUri` (`/rastreo/<codigo>`) and starts login; after login the app opens that route.
No data is shown before authentication. This limitation is documented, not hidden.

**Proposals to confirm:** none beyond the brief. (No pricing, service level or delivery guarantee is stated anywhere in the UI.)

**Convergence overrides:** none (design log history is empty for this project).
Placed: the interaction sits in the first viewport of `/` and `/rastreo` because the language is an operational tool (`tight`-like). The page ends on: the role home or the login button.

**Design principles:**
1. *State over style* — when state clarity conflicts with elegance, state clarity wins: every state has a word, an icon and a shape.
2. *Phone first for the field, desktop first for dispatch* — the repartidor screens use 52 px controls, the despachador screens use 36 px rows; shared tokens, different density classes.
3. *Exact over friendly* — codes, dates and counts are printed in full (`dd-mm-aaaa hh:mm`), never as "hace 2 días".
4. *Motion explains* — GSAP is used only to show order in time (the route line) and never on load of ordinary pages.
5. *One accent* — teal is the action and the travelled route; amber, red, green and blue exist only as status.

## 2. Color

**Model:** OKLCH primitives → semantic roles → component tokens. UI code uses semantic tokens (`var(--color-*)`) only.

| Scale | Hue (°) | Steps | Notes |
|---|---|---|---|
| neutral / neutral-dark | 200 | 1–12 | tinted toward the brand, chroma 0.004–0.012; the dark scale is its own palette |
| brand | 195 | 1–12 | cold teal; step 9 = light action, step 8 = dark action |
| accent | — | — | none; the accent is the brand hue (rule: accent within ~40° of brand) |
| status | 150 / 80 / 25 / 250 | 3, 9, 11 (+ dark 3, 9, 11) | success / warning / danger / info; chroma matched to brand |

Semantic roles: see `tokens/semantic.json` and `semantic.dark.json` (surface base/raised/sunken/overlay/inverse, text primary/secondary/tertiary/on-action/link, border subtle/strong/focus,
action primary/hover/pressed/secondary/destructive, status success/warning/danger/info with fill/surface/text, `route.line`, `route.line-done`, `route.node-pending`).

**Package state to status mapping** (word + icon always accompany colour):

| Estado | Role | Node shape |
|---|---|---|
| Registrado | neutral | hollow circle |
| En centro | info | square node |
| En reparto | brand (action) | filled circle, pulse ring when current |
| Entregado | success | check node |
| Reprogramado (failed attempt) | warning | triangle node, label "Intento fallido" |
| Devuelto a origen | danger | return-arrow node |

**Dark mode rules:** its own palette (not an inversion); elevation by lightness step; text off-white L≈0.94; action chroma reduced; `text.on-action` is dark on the lighter dark-mode fill.

**Contrast policy:** WCAG 2.2 AA everywhere. `contrast.py --tokens` passes (exit 0, light and dark); extra pairs in `design/contrast-pairs.txt`.
APCA is advisory: dark-mode secondary text reads `large-text-only` at Lc 65–68, so body copy in dark mode uses `text.primary`, secondary text is reserved for metadata at ≥ 14 px.

## 3. Typography

| Role | Family | Why (tied to attributes) | Weights loaded |
|---|---|---|---|
| Display + text | Hanken Grotesk | Neo-grotesque, low-contrast and calm: reads "exact" without the watch-list faces; full Latin-ext for Spanish accents | 400, 500, 600, 700 |
| Mono | IBM Plex Mono | Codes and timestamps are the data; mono with tabular figures aligns columns and evokes the waybill | 400, 500, 600 |

Loaded through one Google Fonts stylesheet in `index.html` (`display=swap`, preconnect). This is a CSS request only, no external script. Fallback is `system-ui`. Production: self-host and subset.

**Scale (Operate, fluid):** display 36→56 px (tracking code, one per page), h1 28→38, h2 22→28, h3 18→21, h4 17, body 16→17, body-lg 17→19, label 14, caption 12, code 14, mono-data 15.
Rules: measure ≤ 65ch; `text-wrap: balance` on headings; `tabular-nums` on numbers; real quotes; never letter-space lowercase.

**Script coverage:** Latin extended (á é í ó ú ñ ü ¿ ¡). Fallback `system-ui`.

## 4. Spacing, sizing, layout

- Base unit 4 px, scale `--space-0…15` (0, 2, 4, 6, 8, 12, 16, 20, 24, 32, 40, 48, 64, 80, 96, 128).
- Control heights 36 (dense, desktop tables) / 44 (default, minimum target) / 52 (repartidor primary actions). Targets ≥ 44 px except in dense desktop tables where the row is the target.
- Layout: content max 1280 px, reading width 65ch, page margin `--space-margin-page`. Breakpoints named by layout change: `narrow 600`, `regular 905` (full nav for roles with up to four links), `wide 1240` (full nav for the despachador, six links; the role word beside the user name is visually hidden there so the bar stays on one line).
- Radius hierarchy (square language): control 4 px, card 6 px, sheet 8 px, pill for badges only.
- Elevation: hairline borders first; `shadow.overlay` only on menus, toasts and dialogs; dark mode by lightness step.

## 5. Motion

| Token | Value | Use |
|---|---|---|
| `duration.instant` | 80 ms | press feedback |
| `duration.fast` | 120 ms | hover, toggles |
| `duration.base` | 180 ms | menus, toasts |
| `duration.slow` | 280 ms | route reveal per row |
| `duration.slower` | 420 ms | route line draw |
| `ease.standard` / `decelerate` / `accelerate` | cubic-bezier | most / entering / exiting |

Signature moments: (1) route line draws and events stagger in (GSAP, transform and opacity only, ≤ 0.9 s total); (2) the current node's ring pulses once.
Everything else is CSS transitions on colour and opacity. `prefers-reduced-motion: reduce` → no GSAP tweens run, the final state is rendered immediately and the pulse is static (`gsap.matchMedia`).

## 6. Iconography & imagery

- Icon set: a small inline SVG set drawn in `src/components/Icon.jsx` (24 px grid, 1.75 px stroke, `currentColor`, `aria-hidden` when next to text). No emoji, no icon font.
- No illustration or photography. Empty and error states are typographic: an icon, a heading, one sentence, one action.

## 7. Components (index)

| Component | Location | Status |
|---|---|---|
| Button, LinkButton | `src/components/ui/Button.jsx` | done |
| Field (label, hint, error), TextInput, Select, Textarea | `src/components/ui/Field.jsx` | done |
| StatusBadge (word + icon + colour) | `src/components/ui/StatusBadge.jsx` | done |
| Loading / EmptyState / ErrorState | `src/components/ui/States.jsx` | done |
| Toast provider and `useToast` | `src/components/ui/Toast.jsx` | done |
| Layout (header, role nav, skip link, theme toggle) | `src/components/layout/` | done |
| ShipmentTimeline + PackageHeader | `src/components/shipment/` | done |
| ShipmentCode (short / grouped display of `G5-` + 32 hex codes), CopyCodeButton | `src/components/shipment/` | done |
| DataTable, dialog / confirm | `src/components/ui/` | done |
| ViewFreshnessNote (materialized-view schedule) | `src/components/reports/` | done |

## 8. Voice & microcopy

Tone in three words: direct, precise, courteous. Sentence case. Verb-first buttons ("Rastrear envío", "Marcar como entregado"). Errors say what happened and what to do ("No encontramos el código TRK-… Revisa que esté completo."). Use "tú"-neutral imperatives ("Ingresa tu código") rather than "usted" or voseo. Banned: "¡Bienvenido!" decoration, "oops", exclamation marks in errors, "revolucionario", "soluciones logísticas".

## 9. Platform notes

- **Web:** CSS custom properties from `src/styles/tokens.css`; `color-scheme: light dark`; theme override with `data-theme="light|dark"` on `<html>`; focus ring 2 px `--color-focus` with offset; `env(safe-area-inset-*)` on the sticky header and bottom bar.

## 10. Never list

- No gradients on buttons, no glow, no glass, no gradient text.
- No icon-in-circle feature rows, no 3-card grid on the landing.
- No colour-only status. No auto-playing motion outside the timeline reveal.
- No literal colours, sizes or font names in component CSS: tokens only.
- No horizontal progress bar as the tracking anchor (it hides failed attempts).

## 11. Review of record

Full pass (F1-F7), 2026-10-31, Standard mode. Evidence: Playwright with Chrome for Testing against the real stack (PostgreSQL 16, Keycloak 26.4, the repo jar), light and dark, 1280 and 375 px; `crit_board.py` rendered a static snapshot of the signed-in tracking screen (the SPA needs a login, so the board cannot open it directly).

| Gate | Grade | One line |
|---|---|---|
| 1 Brief & register honoured | A | R2, Operate, diagram anchor, no imagery. On wide screens the package header and the route line now share the first viewport |
| 3 Hierarchy & composition | B | One focal point per screen; the repartidor and despachador screens are list-led and deliberately plain. Tracking result: code at h1 scale left, route line right |
| 5 Colour & contrast | A | `contrast.py --tokens` exit 0 (light and dark); dark mode checked on every role home, tracking, asignar ruta and reportes: no clipped or low-contrast text |
| 6 Spacing, layout, responsive | A | `scrollWidth == clientWidth` at 375 px on 17 routes; header fits on one line at 1240, 1280, 1440 and 1920 px and collapses to the menu button below 1240 px (despachador) or 905 px (other roles) |
| 10 Accessibility | A- | Keyboard walk, focus ring 2 px on every tab stop, native `<dialog>` trap and focus return, form errors linked by `aria-describedby`, `aria-live` on results and success banners, touch targets at least 44 px at 375 px. No screen-reader pass |
| 13 Slop & originality | A | `slop_lint.py` grade A, 0.0 findings per 1k lines |

Fixed in this review: nav labels never wrap and the bar collapses at a width where it would not fit; tracking codes (`G5-` + 32 hex) have one display component (`ShipmentCode`: `G5-3f9a…b2c1` in lists with the full code in the tooltip and for screen readers, eight blocks of four in headers, selection copy and the copy button both yield the exact original string) and the search strips whitespace; nested routes keep their nav item active; the performance note states the materialized-view schedule (the API does not expose the last refresh time, so none is shown); the CRUD success banner appears only after the list reload; kg and m³ come from `lib/format.js` with a no-break space; phone touch targets raised to 44 px.

**Gate 14 (studio crit), tracking screen:** Idea 4 (the waybill route line is specific to dispatch), Memorability 4 (code and route line in one first viewport; was 3 while the route sat below the fold), Imagery & craft 4 (diagram and grouped code are finished; no imagery by design), Type & rhythm 4 (mono code against grotesk, fixed time column), Completeness 5 (all content is live API data), Interaction 4 (paste with or without spaces, the line draws in, copy and shareable URL). **Total 25 of 30, below the A+ bar of 26**; recorded with `design_log.py add --crit 25`. The board held no earlier page to compare against (the one logged entry is this project). One more pass would go to the repartidor and despachador screens, which are plainer than the tracking anchor.

Scripts: `slop_lint.py` A · `contrast.py --tokens` exit 0 · `design_log.py add` recorded (notes: light surface and diagram result repeat only because this project is its own history; six departures from the market line are written in section 1).
Previous run compared: foundation pass 2026-10-31 (landing and tracking only); the tracking first viewport is stronger (route line no longer below the fold), nothing regressed.
Studio test: a studio would likely add a map-free overview of every route on the despachador home and theme the Keycloak login page. Open items: Keycloak login page is unthemed (English); fonts come from Google Fonts; no screen-reader pass; `shoot.py` cannot log in, so Playwright was used.

## 12. Changelog

| Version | Date | Change |
|---|---|---|
| 0.1.0 | 2026-10-31 | initial direction, tokens and tracking anchor |
| 0.2.0 | 2026-10-31 | review pass: nav breakpoints, ShipmentCode component, tracking result layout, touch targets, section 11 |
