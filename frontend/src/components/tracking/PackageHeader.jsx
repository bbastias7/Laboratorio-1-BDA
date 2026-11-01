import { formatDate, formatDateTime } from '../../lib/format.js';
import { StatusBadge } from '../ui/StatusBadge.jsx';
import { CopyCodeButton } from './CopyCodeButton.jsx';
import { TrackingCode } from './TrackingCode.jsx';

/**
 * Header of a tracking result: code (mono, poster scale), state badge, destination, due date, attempts.
 * Props: tracking (TrackingResponse from api/tracking.js).
 */
export function PackageHeader({ tracking }) {
  const { codigo, estado, direccionDestino, fechaCompromiso, entregadoAt, intentos, fechaReprogramada } = tracking;
  return (
    <section className="pkg" aria-labelledby="pkg-code">
      <div className="pkg-top">
        <div>
          <p className="eyebrow" id="pkg-label">Código de seguimiento</p>
          <h2 id="pkg-code" className="pkg-code" aria-describedby="pkg-label"><TrackingCode code={codigo} variant="full" /></h2>
        </div>
        <div className="pkg-state">
          <StatusBadge estado={estado} size="lg" />
          <CopyCodeButton value={codigo} />
        </div>
      </div>
      <dl className="facts">
        <div className="fact fact-wide">
          <dt>Destino</dt>
          <dd>{direccionDestino}</dd>
        </div>
        <div className="fact">
          <dt>Fecha compromiso</dt>
          <dd className="mono">{formatDate(fechaCompromiso)}</dd>
        </div>
        <div className="fact">
          <dt>Intentos de entrega</dt>
          <dd className="mono">{intentos ?? 0}</dd>
        </div>
        {fechaReprogramada && estado === 'Reprogramado' && (
          <div className="fact">
            <dt>Nueva fecha</dt>
            <dd className="mono">{formatDate(fechaReprogramada)}</dd>
          </div>
        )}
        {entregadoAt && (
          <div className="fact">
            <dt>Entregado el</dt>
            <dd className="mono">{formatDateTime(entregadoAt)}</dd>
          </div>
        )}
      </dl>
    </section>
  );
}
