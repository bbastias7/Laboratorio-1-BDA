import { Link, useParams } from 'react-router-dom';
import { centros, entregas, rutas } from '../../api';
import { useAsync } from '../../hooks/useAsync.js';
import { useDocumentTitle } from '../../hooks/useDocumentTitle.js';
import { PageHeader } from '../../components/layout/PageHeader.jsx';
import { LinkButton } from '../../components/ui/Button.jsx';
import { DataTable } from '../../components/ui/DataTable.jsx';
import { StatusBadge } from '../../components/ui/StatusBadge.jsx';
import { EmptyState, ErrorState, Loading, errorKind } from '../../components/ui/States.jsx';
import { formatDate, formatDateTime } from '../../lib/format.js';
import { ShipmentCode } from '../../components/shipment/ShipmentCode.jsx';
import { ResultadoMark } from './RepartidorHome.jsx';

/** Mis rutas: GET /api/ruta is already limited to the caller's own routes. */
export function RepartidorRutas() {
  useDocumentTitle('Mis rutas');
  const { data, error, loading, reload } = useAsync(async (signal) => {
    const [lista, cds] = await Promise.all([rutas.listRutas(signal), centros.listCentros(signal)]);
    return { lista, centros: Object.fromEntries(cds.map((c) => [c.centroDistId, c.nombre])) };
  }, []);

  const rows = data ? [...data.lista].sort((a, b) => b.rutaId - a.rutaId) : [];
  const columns = [
    { key: 'rutaId', header: 'Ruta', mono: true, primary: true, render: (r) => <Link to={`/repartidor/rutas/${r.rutaId}`}>Ruta {r.rutaId}</Link> },
    { key: 'fecha', header: 'Fecha', mono: true, render: (r) => formatDateTime(r.fecha) },
    { key: 'centro', header: 'Centro', render: (r) => data.centros[r.centroDistId] ?? `Centro ${r.centroDistId}` },
  ];

  return (
    <div className="page">
      <PageHeader title="Mis rutas" eyebrow="Repartidor" actions={<LinkButton to="/repartidor" icon="truck">Mis entregas</LinkButton>} />
      {loading && !data && <Loading label="Cargando tus rutas" />}
      {error && <ErrorState kind={errorKind(error)} title="No pudimos cargar tus rutas" message={error.message} action={{ label: 'Reintentar', onClick: reload }} />}
      {data && rows.length === 0 && (
        <EmptyState icon="route" title="Aún no tienes rutas" message="Un despachador te asignará una ruta con sus paquetes." />
      )}
      {data && rows.length > 0 && (
        <DataTable caption="Mis rutas" columns={columns} rows={rows} rowKey={(r) => r.rutaId} />
      )}
    </div>
  );
}

/** One route with its packages (read only; the field actions live in Mis entregas). */
export function RepartidorRutaDetalle() {
  const { id } = useParams();
  useDocumentTitle(`Ruta ${id}`);
  const { data, error, loading, reload } = useAsync(async (signal) => {
    const [ruta, items] = await Promise.all([rutas.getRuta(id, signal), entregas.listEntregas({ soloPendientes: false }, signal)]);
    return { ruta, items: items.filter((e) => String(e.rutaId) === String(id)) };
  }, [id]);

  const columns = [
    { key: 'codigo', header: 'Código', mono: true, primary: true, render: (e) => <Link to={`/rastreo/${encodeURIComponent(e.codigo)}`}><ShipmentCode code={e.codigo} /></Link> },
    { key: 'direccionDestino', header: 'Destino' },
    { key: 'resultado', header: 'Resultado', render: (e) => <ResultadoMark resultado={e.resultado} /> },
    { key: 'estado', header: 'Estado', render: (e) => <StatusBadge estado={e.estado} /> },
    { key: 'intentos', header: 'Intentos fallidos', numeric: true },
  ];

  return (
    <div className="page">
      <PageHeader
        title={`Ruta ${id}`}
        eyebrow="Mis rutas"
        actions={<LinkButton to="/repartidor/rutas" icon="route">Volver a mis rutas</LinkButton>}
      >
        {data ? `Fecha ${formatDate(data.ruta.fecha)} · ${data.items.length} ${data.items.length === 1 ? 'paquete' : 'paquetes'}` : undefined}
      </PageHeader>
      {loading && !data && <Loading label="Cargando la ruta" />}
      {error && (
        <ErrorState
          kind={errorKind(error)}
          title={error.kind === 'forbidden' ? 'Esta ruta es de otro repartidor' : 'No pudimos cargar la ruta'}
          message={error.kind === 'forbidden' ? 'Solo puedes ver tus propias rutas.' : error.message}
          action={error.kind === 'forbidden' ? { label: 'Ir a mis rutas', to: '/repartidor/rutas' } : { label: 'Reintentar', onClick: reload }}
        />
      )}
      {data && data.items.length === 0 && <EmptyState icon="box" title="Esta ruta no tiene paquetes" />}
      {data && data.items.length > 0 && (
        <DataTable caption={`Paquetes de la ruta ${id}`} columns={columns} rows={data.items} rowKey={(e) => e.paqueteId} />
      )}
    </div>
  );
}
