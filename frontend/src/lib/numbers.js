// Decimal parsing for es-CL users: both "2,5" and "2.5" are accepted; the API receives a JSON number.

/** "2,5" -> 2.5; NaN when the text is not a plain non-negative decimal. */
export function parseDecimal(raw) {
  const s = String(raw ?? '').trim().replace(',', '.');
  return /^\d+(\.\d+)?$/.test(s) ? Number(s) : Number.NaN;
}

/** Validation message for a positive decimal with at most 2 decimals (NUMERIC(10,2) in the database), or null. */
export function positiveDecimalError(raw, label) {
  const s = String(raw ?? '').trim();
  if (!s) return `Ingresa ${label}.`;
  const n = parseDecimal(s);
  if (Number.isNaN(n)) return `${label[0].toUpperCase()}${label.slice(1)} debe ser un número, por ejemplo 2,5.`;
  if (n <= 0) return `${label[0].toUpperCase()}${label.slice(1)} debe ser mayor que 0.`;
  if (!/^\d+([.,]\d{1,2})?$/.test(s)) return `${label[0].toUpperCase()}${label.slice(1)} admite hasta 2 decimales.`;
  if (n > 99999999.99) return `${label[0].toUpperCase()}${label.slice(1)} es demasiado grande.`;
  return null;
}

const pad = (n) => String(n).padStart(2, '0');
const toIsoDate = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

/** Local date "yyyy-mm-dd" of today plus `days` (used for <input type="date"> bounds). */
export const isoDateFromToday = (days = 0) => {
  const d = new Date();
  d.setDate(d.getDate() + days);
  return toIsoDate(d);
};

/** Current local date and time as a <input type="datetime-local"> value ("yyyy-mm-ddThh:mm"). */
export const nowLocalInput = () => {
  const d = new Date();
  return `${toIsoDate(d)}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
};
