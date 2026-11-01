import { auditoria, paquetes, usuarios } from '../../api';
import { useAsync } from '../../hooks/useAsync.js';
import { useDocumentTitle } from '../../hooks/useDocumentTitle.js';
import { PageHeader } from '../../components/layout/PageHeader.jsx';
import { Icon } from '../../components/Icon.jsx';
import { DataTable } from '../../components/ui/DataTable.jsx';
import { EmptyState, ErrorState, Loading, errorKind } from '../../components/ui/States.jsx';
import { TrackingCode } from '../../components/tracking/TrackingCode.jsx';
import { formatDateTime } from '../../lib/format.js';

/** Read-only log of destination changes made after dispatch (GET /api/auditoria). */
export function Auditoria() {
  useDocumentTitle('Auditoría');
  const { data, error, loading, reload } = useAsync(async (signal) => {
    const [lista, pk, us] = await Promise.all([auditoria.listAuditoria(signal), paquetes.listPaquetes({}, signal), usuarios.listUsuarios(signal)]);
    return {
      lista: [...lista].sort((a, b) => b.auditoriaId - a.auditoriaId),
      codigos: Object.fromEntries(pk.map((p) => [p.paqueteId, p.codigo])),
      nombres: Object.fromEntries(us.map((u) => [u.usuarioId, u.nombre])),
    };
  }, []);

  const columns = data && [
    { key: 'fecha', header: 'Fecha', mono: true, primary: true, render: (a) => formatDateTime(a.fecha) },
    { key: 'paqueteId', header: 'Paquete', mono: true, render: (a) => (data.codigos[a.paqueteId] ? <TrackingCode code={data.codigos[a.paqueteId]} /> : `Paquete ${a.paqueteId}`) },
    {
      key: 'cambio', header: 'Dirección anterior → nueva',
      render: (a) => (
        <span>
          <span>{a.direccionAnterior}</span> <Icon name="chevron" size="sm" label="cambió a" /> <strong>{a.direccionNueva}</strong>
        </span>
      ),
    },
    { key: 'usuarioId', header: 'Usuario', render: (a) => data.nombres[a.usuarioId] ?? `Usuario ${a.usuarioId}` },
  ];

  return (
    <div className="page">
      <PageHeader title="Auditoría" eyebrow="Despachador">
        Cambios de dirección de destino hechos después de despachar un paquete. El registro es de solo lectura.
      </PageHeader>
      {loading && !data && <Loading label="Cargando la auditoría" />}
      {error && <ErrorState kind={errorKind(error)} title="No pudimos cargar la auditoría" message={error.message} action={{ label: 'Reintentar', onClick: reload }} />}
      {data && data.lista.length === 0 && (
        <EmptyState icon="clock" title="Sin cambios registrados" message="Aquí aparecerán los cambios de destino hechos sobre paquetes ya despachados." />
      )}
      {data && data.lista.length > 0 && (
        <DataTable dense caption="Cambios de dirección" columns={columns} rows={data.lista} rowKey={(a) => a.auditoriaId} />
      )}
    </div>
  );
}
