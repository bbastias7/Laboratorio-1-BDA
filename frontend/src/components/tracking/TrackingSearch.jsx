import { useState } from 'react';
import { Icon } from '../Icon.jsx';
import { normalizeCode } from '../../lib/code.js';

/**
 * Tracking code field (the signature interaction's entry). Props:
 * - initialValue: prefilled code (from the URL)
 * - onSubmit(code): called with the trimmed code
 * - submitLabel: button text ("Rastrear envío" by default; "Ingresar para rastrear" for anonymous users)
 * - busy: disables the button while a request runs
 * - autoFocus
 */
export function TrackingSearch({ initialValue = '', onSubmit, submitLabel = 'Rastrear envío', busy = false, autoFocus = false }) {
  const [value, setValue] = useState(initialValue);
  const [error, setError] = useState('');

  const submit = (e) => {
    e.preventDefault();
    const code = normalizeCode(value); // grouped or pasted codes work with or without spaces
    if (!code) { setError('Escribe el código de seguimiento de tu envío.'); return; }
    if (code.length > 64) { setError('El código no puede tener más de 64 caracteres, sin contar los espacios.'); return; }
    setError('');
    onSubmit(code);
  };

  return (
    <form className="search" onSubmit={submit} role="search" aria-label="Rastrear un envío" noValidate>
      <label htmlFor="codigo" className="search-label">Código de seguimiento</label>
      <div className="search-row">
        <input
          id="codigo"
          name="codigo"
          className="control control-lg control-mono"
          type="text"
          inputMode="text"
          autoComplete="off"
          autoCapitalize="characters"
          spellCheck="false"
          autoFocus={autoFocus}
          placeholder="Ej.: G5-DEMO-1"
          value={value}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? 'codigo-err' : 'codigo-hint'}
          onChange={(e) => { setValue(e.target.value); if (error) setError(''); }}
        />
        <button type="submit" className="btn btn-primary btn-lg" disabled={busy} aria-busy={busy || undefined}>
          <Icon name="search" />
          <span>{busy ? 'Buscando…' : submitLabel}</span>
        </button>
      </div>
      {error
        ? <p id="codigo-err" className="field-msg" role="alert">{error}</p>
        : <p id="codigo-hint" className="field-hint">Pégalo tal cual, con o sin espacios. Lo encuentras en la etiqueta de tu envío.</p>}
    </form>
  );
}
