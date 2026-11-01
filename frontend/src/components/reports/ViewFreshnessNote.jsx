import { Icon } from '../Icon.jsx';

/**
 * Honest note about where performance figures come from. The API only returns the refresh time as the answer to
 * POST /api/reportes/refrescar; it never exposes when the view was last refreshed, so we state the schedule only.
 * Props: refreshedAt (LocalDateTime string from a refresh done in this session, optional).
 */
export function ViewFreshnessNote({ children }) {
  return (
    <p className="notice view-note">
      <Icon name="info" size="sm" />
      <span>
        Estas cifras vienen de una vista materializada que se refresca sola todos los días a las 03:00 (hora de Santiago) o cuando usas «Refrescar vista».
        Pueden no incluir las entregas de las últimas horas. La API no informa cuándo fue el último refresco.
        {children}
      </span>
    </p>
  );
}
