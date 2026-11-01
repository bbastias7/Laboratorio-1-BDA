import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { paquetes } from '../../api';
import { useAuth } from '../../auth/AuthProvider.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import { useDocumentTitle } from '../../hooks/useDocumentTitle.js';
import { PageHeader } from '../../components/layout/PageHeader.jsx';
import { LinkButton } from '../../components/ui/Button.jsx';
import { Field, Select } from '../../components/ui/Field.jsx';
import { DataTable } from '../../components/ui/DataTable.jsx';
import { StatusBadge, ESTADOS } from '../../components/ui/StatusBadge.jsx';
import { EmptyState, ErrorState, Loading, errorKind } from '../../components/ui/States.jsx';
import { formatDate } from '../../lib/format.js';
import { ShipmentCode } from '../../components/shipment/ShipmentCode.jsx';

/** Mis envíos: the client's own packages (the API already scopes GET /api/paquete to the caller). */
export function ClienteHome() {
  const { user } = useAuth();
  useDocumentTitle('Mis envíos');
  const { data, error, loading, reload } = useAsync((signal) => paquetes.listPaquetes({}, signal), []);
  const [estado, setEstado] = useState('');

  const sorted = useMemo(() => [...(data ?? [])].sort((a, b) => b.paqueteId - a.paqueteId), [data]);
  const counts = useMemo(() => {
    const c = {};
    sorted.forEach((p) => { c[p.estado] = (c[p.estado] ?? 0) + 1; });
    return c;
  }, [sorted]);
  const visible = estado ? sorted.filter((p) => p.estado === estado) : sorted;

  const columns = [
    {
      key: 'codigo', header: 'Código', mono: true, primary: true,
      render: (p) => <Link to={`/cliente/envios/${p.paqueteId}`}><ShipmentCode code={p.codigo} /></Link>,
    },
    { key: 'estado', header: 'Estado', render: (p) => <StatusBadge estado={p.estado} /> },
    { key: 'direccionDestino', header: 'Destino' },
    { key: 'fechaCompromiso', header: 'Compromiso', mono: true, render: (p) => formatDate(p.fechaCompromiso) },
  ];

  return (
    <div className="page">
      <PageHeader
        title="Mis envíos"
        eyebrow={`Hola, ${user.name}`}
        actions={<LinkButton to="/cliente/nuevo" variant="primary" icon="plus">Crear envío</LinkButton>}
      />

      {loading && !data && <Loading label="Cargando tus envíos" />}
      {error && (
        <ErrorState
          kind={errorKind(error)}
          title="No pudimos cargar tus envíos"
          message={error.message}
          action={{ label: 'Reintentar', onClick: reload }}
        />
      )}

      {data && sorted.length === 0 && (
        <EmptyState
          icon="box"
          title="Aún no tienes envíos"
          message="Cuando registres un envío aparecerá aquí con su código de seguimiento."
          action={{ label: 'Crear tu primer envío', to: '/cliente/nuevo' }}
        />
      )}

      {data && sorted.length > 0 && (
        <section className="section" aria-label="Lista de envíos">
          <div className="toolbar">
            <Field label="Filtrar por estado">
              <Select value={estado} onChange={(e) => setEstado(e.target.value)}>
                <option value="">Todos ({sorted.length})</option>
                {Object.keys(ESTADOS).filter((k) => counts[k]).map((k) => (
                  <option key={k} value={k}>{k} ({counts[k]})</option>
                ))}
              </Select>
            </Field>
          </div>
          <p className="sr-only" role="status">{visible.length} envíos mostrados</p>
          {visible.length === 0 ? (
            <EmptyState icon="search" title="Ningún envío con ese estado" message="Prueba con otro estado o quita el filtro." action={{ label: 'Quitar filtro', onClick: () => setEstado('') }} />
          ) : (
            <DataTable
              caption="Mis envíos"
              columns={columns}
              rows={visible}
              rowKey={(p) => p.paqueteId}
              rowActions={(p) => (
                <span className="row-actions">
                  <LinkButton to={`/rastreo/${encodeURIComponent(p.codigo)}`} size="sm" icon="route">Ver seguimiento</LinkButton>
                </span>
              )}
            />
          )}
        </section>
      )}
    </div>
  );
}
