import { Link } from 'react-router-dom';

/** Wordmark: a small route glyph + "Logística G5" set in type (no logo asset exists). */
export function Brand() {
  return (
    <Link to="/" className="brand" aria-label="Logística G5, inicio">
      <svg className="brand-mark" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
        <path d="M12 4v16" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
        <circle cx="12" cy="5" r="3" fill="currentColor" />
        <circle cx="12" cy="19" r="3" fill="none" stroke="currentColor" strokeWidth="2" />
      </svg>
      <span className="brand-text">Logística <strong>G5</strong></span>
    </Link>
  );
}
