import { useEffect, useRef, useState } from 'react';
import { Icon } from '../Icon.jsx';

/** Copies `value` to the clipboard and confirms politely ("Código copiado"). Props: value, label. */
export function CopyCodeButton({ value, label = 'Copiar código' }) {
  const [done, setDone] = useState(false);
  const [failed, setFailed] = useState(false);
  const timer = useRef(null);
  useEffect(() => () => clearTimeout(timer.current), []);

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(value);
      setDone(true); setFailed(false);
    } catch {
      setDone(false); setFailed(true);
    }
    clearTimeout(timer.current);
    timer.current = setTimeout(() => { setDone(false); setFailed(false); }, 2500);
  };

  return (
    <>
      <button type="button" className="btn btn-secondary btn-sm" onClick={copy}>
        <Icon name={done ? 'check' : 'copy'} />
        <span>{done ? 'Copiado' : label}</span>
      </button>
      <span className="sr-only" role="status" aria-live="polite">
        {done ? 'Código copiado al portapapeles' : failed ? 'No se pudo copiar el código' : ''}
      </span>
    </>
  );
}
