import { Link } from 'react-router-dom';
import { Icon } from '../Icon.jsx';
import { Button } from './Button.jsx';

/** Polite loading indicator. Props: label (what is loading), inline (no page-height padding). */
export function Loading({ label = 'Cargando', inline = false }) {
  return (
    <div className={`state state-loading ${inline ? 'state-inline' : ''}`} role="status" aria-live="polite">
      <Icon name="clock" />
      <span>{label}…</span>
    </div>
  );
}

/** Nothing to show. Props: title, message, action {label, to | onClick}. */
export function EmptyState({ title, message, action, icon = 'box' }) {
  return (
    <div className="state state-empty">
      <Icon name={icon} size="lg" />
      <h2 className="state-title">{title}</h2>
      {message && <p className="state-msg">{message}</p>}
      <StateAction action={action} />
    </div>
  );
}

const KIND_ICON = { forbidden: 'lock', notFound: 'search', network: 'wifi', error: 'alert' };

/**
 * Failure state. Props: kind "forbidden" | "notFound" | "network" | "error", title, message, action {label, to | onClick}.
 * Use `errorKind(err)` to map an ApiError.
 * Announced with role="alert".
 */
export function ErrorState({ kind = 'error', title, message, action }) {
  return (
    <div className={`state state-error state-${kind}`} role="alert">
      <Icon name={KIND_ICON[kind] ?? 'alert'} size="lg" />
      <h2 className="state-title">{title ?? defaultTitle(kind)}</h2>
      {message && <p className="state-msg">{message}</p>}
      <StateAction action={action} />
    </div>
  );
}

const defaultTitle = (kind) => ({
  forbidden: 'No autorizado',
  notFound: 'No encontrado',
  network: 'Sin conexión con el servidor',
}[kind] ?? 'Algo salió mal');

/** Maps an ApiError (or any error) to an ErrorState kind. */
export const errorKind = (err) => (['forbidden', 'notFound', 'network'].includes(err?.kind) ? err.kind : 'error');

function StateAction({ action }) {
  if (!action) return null;
  if (action.to) return <Link className="btn btn-secondary btn-md" to={action.to}><span>{action.label}</span></Link>;
  return <Button variant="secondary" onClick={action.onClick}>{action.label}</Button>;
}
