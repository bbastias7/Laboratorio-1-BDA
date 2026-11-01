import { useNavigate, useParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthProvider.jsx';
import { AccountProblem } from '../auth/guards.jsx';
import { rastrear } from '../api/rastreo.js';
import { useAsync } from '../hooks/useAsync.js';
import { useDocumentTitle } from '../hooks/useDocumentTitle.js';
import { PageHeader } from '../components/layout/PageHeader.jsx';
import { ErrorState, Loading, errorKind } from '../components/ui/States.jsx';
import { normalizeCode, shortCode } from '../lib/code.js';
import { PackageHeader } from '../components/shipment/PackageHeader.jsx';
import { ShipmentSearch } from '../components/shipment/ShipmentSearch.jsx';
import { ShipmentTimeline } from '../components/shipment/ShipmentTimeline.jsx';

const trackingPath = (code) => `/rastreo/${encodeURIComponent(code)}`;

/** /rastreo and /rastreo/:codigo. Any signed-in role; anonymous visitors are asked to log in and resume the same code. */
export function ShipmentPage() {
  const { codigo: rawCodigo } = useParams();
  const codigo = rawCodigo ? normalizeCode(rawCodigo) : rawCodigo; // pasted codes may carry spaces
  const { status, login } = useAuth();
  const navigate = useNavigate();
  const authed = status === 'authenticated';
  useDocumentTitle(codigo ? `Rastreo ${shortCode(codigo)}` : 'Rastrear envío');

  const { data, error, loading } = useAsync(
    (signal) => (authed && codigo ? rastrear(codigo, signal) : Promise.resolve(null)),
    [authed, codigo],
  );

  if (status === 'error') return <AccountProblem />;

  const onSubmit = (code) => (authed ? navigate(trackingPath(code)) : login(trackingPath(code)));

  return (
    <div className="page page-tracking">
      <PageHeader title="Rastrear un envío" eyebrow="Seguimiento">
        Escribe el código y revisa por dónde ha pasado tu paquete.
      </PageHeader>

      <ShipmentSearch
        key={codigo ?? 'empty'}
        initialValue={codigo ?? ''}
        onSubmit={onSubmit}
        busy={authed && loading}
        submitLabel={authed ? 'Rastrear envío' : 'Ingresar para rastrear'}
        autoFocus={!codigo}
      />

      {status === 'loading' && <Loading label="Verificando tu sesión" />}

      {status === 'anonymous' && (
        <p className="notice">
          Para ver el estado de un envío necesitas iniciar sesión.
          {codigo ? ` Cuando ingreses abriremos el código ${shortCode(codigo)}.` : ''}
        </p>
      )}

      <div className="result" aria-live="polite" aria-atomic="true">
        {authed && codigo && loading && <Loading label={`Buscando ${shortCode(codigo)}`} />}
        {authed && error && <TrackingError error={error} codigo={codigo} />}
        {authed && data && !loading && (
          <>
            <p className="sr-only">
              {`Envío ${data.codigo}: ${data.estado}. ${data.eventos.length} eventos registrados.`}
            </p>
            <PackageHeader tracking={data} />
            <ShipmentTimeline tracking={data} />
          </>
        )}
      </div>
    </div>
  );
}

function TrackingError({ error, codigo }) {
  if (error.kind === 'notFound') {
    return (
      <ErrorState
        kind="notFound"
        title="No encontramos ese código"
        message={`No hay un envío con el código ${shortCode(codigo)}. Revisa que esté completo y vuelve a intentarlo.`}
      />
    );
  }
  if (error.kind === 'forbidden') {
    return (
      <ErrorState
        kind="forbidden"
        title="No autorizado para ver este envío"
        message="Este envío pertenece a otro cliente. Solo puedes rastrear tus propios envíos."
      />
    );
  }
  return <ErrorState kind={errorKind(error)} message={error.message} />;
}
