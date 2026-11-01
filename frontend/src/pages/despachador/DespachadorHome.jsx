import { useMemo } from 'react';
import { Link } from 'react-router-dom';
import { paquetes, reportes, rutaPaquete, rutas } from '../../api';
import { useAuth } from '../../auth/AuthProvider.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import { useDocumentTitle } from '../../hooks/useDocumentTitle.js';
import { PageHeader } from '../../components/layout/PageHeader.jsx';
import { Icon } from '../../components/Icon.jsx';
import { ViewFreshnessNote } from '../../components/reports/ViewFreshnessNote.jsx';
import { LinkButton } from '../../components/ui/Button.jsx';
import { ESTADOS, StatusBadge } from '../../components/ui/StatusBadge.jsx';
import { ErrorState, Loading, errorKind } from '../../components/ui/States.jsx';

const ELEGIBLES = ['Registrado', 'En centro', 'Reprogramado'];
const UMBRAL = 85;

/** Panel de operaciones: every figure is computed from the API responses on screen; nothing is estimated. */
export function DespachadorHome() {
  const { user } = useAuth();
  useDocumentTitle('Operación');
  const pk = useAsync((s) => paquetes.listPaquetes({}, s), []);
  const rt = useAsync((s) => rutas.listRutas(s), []);
  const rp = useAsync((s) => rutaPaquete.listRutaPaquetes(s), []);
  const al = useAsync((s) => reportes.alertas(UMBRAL, s), []);

  const porEstado = useMemo(() => {
    const c = Object.fromEntries(Object.keys(ESTADOS).map((k) => [k, 0]));
    (pk.data ?? []).forEach((p) => { c[p.estado] = (c[p.estado] ?? 0) + 1; });
    return c;
  }, [pk.data]);
  const total = pk.data?.length ?? 0;
  const pendientes = useMemo(() => {
    const enRuta = new Set((rp.data ?? []).filter((x) => x.resultado === 'Pendiente').map((x) => x.paqueteId));
    return (pk.data ?? []).filter((p) => ELEGIBLES.includes(p.estado) && !enRuta.has(p.paqueteId)).length;
  }, [pk.data, rp.data]);
  const entregasPendientes = (rp.data ?? []).filter((x) => x.resultado === 'Pendiente').length;

  return (
    <div className="page">
      <PageHeader
        title="Operación"
        eyebrow={`Hola, ${user.name}`}
        actions={<LinkButton to="/despachador/rutas/asignar" variant="primary" icon="route">Asignar ruta</LinkButton>}
      />

      <div className="ops">
        <div className="ops-col">
          <section className="section" aria-labelledby="espera-title">
            <h2 id="espera-title" className="sr-only">Paquetes que esperan ruta</h2>
            {(pk.loading && !pk.data) || (rp.loading && !rp.data) ? <Loading label="Calculando paquetes por asignar" /> : null}
            {(pk.error || rp.error) && (
              <ErrorState
                kind={errorKind(pk.error ?? rp.error)}
                title="No pudimos cargar los paquetes"
                message={(pk.error ?? rp.error).message}
                action={{ label: 'Reintentar', onClick: () => { pk.reload(); rp.reload(); } }}
              />
            )}
            {pk.data && rp.data && (
              <div className="focal">
                <span className="focal-number" aria-hidden="true">{pendientes}</span>
                <div className="focal-text">
                  <p><strong>{pendientes === 1 ? 'paquete espera' : 'paquetes esperan'} una ruta</strong></p>
                  <p>Registrados, en centro o reprogramados, sin asignación pendiente.</p>
                  {pendientes > 0 && <Link to="/despachador/rutas/asignar">Asignarlos a una ruta</Link>}
                </div>
              </div>
            )}
          </section>

          {pk.data && (
            <section className="section" aria-labelledby="estados-title">
              <div className="section-head">
                <h2 id="estados-title">Paquetes por estado</h2>
                <p>{total} {total === 1 ? 'paquete' : 'paquetes'} en total</p>
              </div>
              <div className="estado-rows" role="list">
                {Object.keys(porEstado).map((k) => (
                  <div className="estado-row" role="listitem" key={k}>
                    <StatusBadge estado={k} />
                    <span className="estado-count">{porEstado[k]}</span>
                    <div className="bar" aria-hidden="true"><span style={{ '--pct': total ? Math.round((porEstado[k] / total) * 100) : 0 }} /></div>
                  </div>
                ))}
              </div>
              <p><Link to="/despachador/mantenedores/paquetes">Ver y editar paquetes</Link></p>
            </section>
          )}
        </div>

        <div className="ops-col">
          <section className="section" aria-labelledby="rutas-title">
            <h2 id="rutas-title">Rutas y entregas</h2>
            {(rt.loading && !rt.data) && <Loading label="Cargando rutas" inline />}
            {rt.error && <ErrorState kind={errorKind(rt.error)} title="No pudimos cargar las rutas" message={rt.error.message} action={{ label: 'Reintentar', onClick: rt.reload }} />}
            {rt.data && rp.data && (
              <dl className="kv">
                <div><dt>Rutas registradas</dt><dd>{rt.data.length}</dd></div>
                <div><dt>Entregas pendientes en ruta</dt><dd>{entregasPendientes}</dd></div>
                <div><dt>Entregas resueltas</dt><dd>{rp.data.length - entregasPendientes}</dd></div>
              </dl>
            )}
            <p><Link to="/despachador/mantenedores/rutas">Ver rutas</Link></p>
          </section>

          <section className="section" aria-labelledby="alertas-home-title">
            <h2 id="alertas-home-title">Alertas de rendimiento</h2>
            {al.loading && !al.data && <Loading label="Cargando alertas" inline />}
            {al.error && <ErrorState kind={errorKind(al.error)} title="No pudimos cargar las alertas" message={al.error.message} action={{ label: 'Reintentar', onClick: al.reload }} />}
            {al.data && (
              al.data.repartidores.length + al.data.centros.length === 0 ? (
                <p className="flag flag-ok"><Icon name="check" size="sm" />Nadie está bajo {UMBRAL} % de entregas a tiempo.</p>
              ) : (
                <p className="flag flag-low">
                  <Icon name="alert" size="sm" />
                  {al.data.repartidores.length} {al.data.repartidores.length === 1 ? 'repartidor' : 'repartidores'} y {al.data.centros.length} {al.data.centros.length === 1 ? 'centro' : 'centros'} bajo {UMBRAL} % a tiempo.
                </p>
              )
            )}
            <ViewFreshnessNote />
            <p><Link to="/despachador/reportes">Revisar reportes</Link></p>
          </section>
        </div>
      </div>
    </div>
  );
}
