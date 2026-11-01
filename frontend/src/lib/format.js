// es-CL formatting helpers. The backend sends LocalDateTime strings without a zone; the server and
// database run on America/Santiago (spring.jackson.time-zone), so the wall-clock text is already Santiago time.
// We therefore format the components as they are (via UTC, no conversion) instead of letting the
// browser shift them to the viewer's zone.

const LOCALE = 'es-CL';
const NAIVE = /^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2}))?)?/;

/** Parses "2026-11-05T18:00:00" into a Date holding that wall time as UTC components, or null. */
export function parseLocal(value) {
  if (!value) return null;
  const m = NAIVE.exec(String(value));
  if (!m) return null;
  const [, y, mo, d, h = '0', mi = '0', s = '0'] = m;
  return new Date(Date.UTC(+y, +mo - 1, +d, +h, +mi, +s));
}

const dateFmt = new Intl.DateTimeFormat(LOCALE, { timeZone: 'UTC', day: '2-digit', month: '2-digit', year: 'numeric' });
const timeFmt = new Intl.DateTimeFormat(LOCALE, { timeZone: 'UTC', hour: '2-digit', minute: '2-digit', hour12: false });

const joinParts = (fmt, d) => fmt.formatToParts(d).filter((p) => p.type !== 'literal');

/** "05-11-2026" */
export function formatDate(value) {
  const d = parseLocal(value);
  if (!d) return '—';
  const p = Object.fromEntries(joinParts(dateFmt, d).map((x) => [x.type, x.value]));
  return `${p.day}-${p.month}-${p.year}`;
}

/** "18:00" */
export function formatTime(value) {
  const d = parseLocal(value);
  if (!d) return '—';
  const p = Object.fromEntries(joinParts(timeFmt, d).map((x) => [x.type, x.value]));
  return `${p.hour}:${p.minute}`;
}

/** "05-11-2026 18:00" */
export function formatDateTime(value) {
  return parseLocal(value) ? `${formatDate(value)} ${formatTime(value)}` : '—';
}

/** Local wall-clock string for the API from a Date or "yyyy-MM-ddTHH:mm" input value. */
export function toApiDateTime(value) {
  if (!value) return null;
  return /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value) ? `${value}:00` : value;
}

const numberFmt = new Intl.NumberFormat(LOCALE, { maximumFractionDigits: 2 });
/** 12.5 → "12,5" */
export const formatNumber = (n) => (n === null || n === undefined || n === '' ? '—' : numberFmt.format(Number(n)));
/** Units: a no-break space keeps the number and its unit together; the cubic metre uses the real ³ glyph. */
export const formatKg = (n) => `${formatNumber(n)}\u00a0kg`;
export const formatM3 = (n) => `${formatNumber(n)}\u00a0m³`;
/** 87.5 → "87,5 %" */
export const formatPercent = (n) => (n === null || n === undefined ? '—' : `${formatNumber(n)} %`);
