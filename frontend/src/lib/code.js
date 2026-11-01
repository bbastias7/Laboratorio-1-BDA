// Tracking codes are "G5-" + 32 hex characters (database default). They are never altered when copied,
// searched or sent to the API; the helpers below only change how a code is *displayed*.

const LONG = /^(G5)-([0-9a-f]{32})$/i;

/** Removes every whitespace character (pasted codes may arrive grouped or with line breaks). */
export const normalizeCode = (value) => String(value ?? '').replace(/\s+/g, '');

/** "G5-3f9a…b2c1" for long generated codes; shorter codes are returned unchanged. */
export function shortCode(code) {
  const m = LONG.exec(code ?? '');
  return m ? `${m[1]}-${m[2].slice(0, 4)}…${m[2].slice(-4)}` : String(code ?? '');
}

/** { prefix: "G5-", groups: ["3f9a", ...] } for generated codes, otherwise null. */
export function groupCode(code) {
  const m = LONG.exec(code ?? '');
  return m ? { prefix: `${m[1]}-`, groups: m[2].match(/.{4}/g) } : null;
}
