# Assets — Logística G5

Every image, font and icon the UI uses, where it came from and what must be replaced before a real launch.

## Art direction (from `DESIGN.md`)

| Dimension | Decision |
|---|---|
| Anchor type | diagram: the route line (vertical line, one node per event) |
| Subject | The package's own event history from the API |
| People | none |
| Light | n/a |
| Treatment | flat strokes in `route.*` and `status.*` tokens |
| Crop & framing | n/a |
| Ratios | n/a |
| Banned | stock photos of trucks and boxes, map screenshots, emoji, illustrated couriers |

## Photographs used in the prototype

None. No photograph passes the three-match test for an Operate tool with no photography budget; zero is the decision.

## Shot list for the real shoot

None required. If a brand photo set is wanted later, the first useful shot is a repartidor handing over a package at a door, in daylight, 3:2, for the logged-out landing only.

## Video / motion

| Clip | Where | Length | Poster frame | Source | Reduced-motion behaviour |
|---|---|---|---|---|---|
| none | | | | | |

GSAP tweens (route reveal) are code, not assets: `src/components/shipment/ShipmentTimeline.jsx`. Reduced motion renders the final state.

## Illustration, icons, graphic devices

| Item | Style / system | Source or author | Licence | Notes |
|---|---|---|---|---|
| Icon set | 24 px grid, 1.75 px stroke, round caps, `currentColor` | Drawn for this project in `src/components/Icon.jsx` | project code | `aria-hidden` when next to text |
| Route line | CSS borders + inline SVG nodes | project code | project code | the recurring motif, drawn from R5 (Metro de Santiago line diagram) |

## Typefaces

| Role | Family | Weights / axes loaded | Source | Licence | Self-hosted? | Subset |
|---|---|---|---|---|---|---|
| Display + text | Hanken Grotesk | 400 500 600 700 | Google Fonts stylesheet | OFL | no (prototype) | latin, latin-ext (served by Google) |
| Mono | IBM Plex Mono | 400 500 600 | Google Fonts stylesheet | OFL | no (prototype) | latin, latin-ext |

Production: self-host with `@fontsource` or the font files, preload the one used above the fold, `font-display: swap`. Fallback chain is `system-ui`. No external scripts are loaded anywhere; the only third-party request is the fonts stylesheet.

## Logo and brand marks

| Asset | Format | Where it lives | Clear space | Minimum size | Notes |
|---|---|---|---|---|---|
| wordmark "Logística G5" | live text in Hanken Grotesk 700 | `src/components/layout/Brand.jsx` | 1 em | 16 px | no logo exists yet |
| favicon | SVG | `public/favicon.svg` | n/a | n/a | route-line glyph, teal |

## Must be supplied before launch

- [ ] A real logo, if the group wants one.
- [ ] Self-hosted font files (to avoid the Google Fonts request).
- [ ] A public tracking endpoint on the API if anonymous tracking is wanted.

Nothing on screen is invented: all package data comes from the API.
