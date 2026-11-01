import { useEffect, useState } from 'react';
import { Icon } from '../Icon.jsx';

const KEY = 'g5-theme';
const readSaved = () => {
  try { const v = localStorage.getItem(KEY); return v === 'light' || v === 'dark' ? v : null; } catch { return null; }
};
const systemDark = () => window.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;

/** Light/dark switch. Starts from the system preference; the choice is saved in localStorage (best effort). */
export function ThemeToggle() {
  const [theme, setTheme] = useState(readSaved);
  const effective = theme ?? (systemDark() ? 'dark' : 'light');

  useEffect(() => {
    const root = document.documentElement;
    if (theme) root.setAttribute('data-theme', theme);
    else root.removeAttribute('data-theme');
  }, [theme]);

  const toggle = () => {
    const next = effective === 'dark' ? 'light' : 'dark';
    setTheme(next);
    try { localStorage.setItem(KEY, next); } catch { /* storage unavailable: keep the in-memory choice */ }
  };

  return (
    <button type="button" className="icon-btn" onClick={toggle} aria-label={effective === 'dark' ? 'Cambiar a tema claro' : 'Cambiar a tema oscuro'}>
      <Icon name={effective === 'dark' ? 'sun' : 'moon'} />
    </button>
  );
}
