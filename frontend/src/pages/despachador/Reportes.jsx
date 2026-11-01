import { useEffect, useRef, useState } from 'react';
import { reportes } from '../../api';
import { useAsync } from '../../hooks/useAsync.js';
import { useDocumentTitle } from '../../hooks/useDocumentTitle.js';
import { PageHeader } from '../../components/layout/PageHeader.jsx';
import { Icon } from '../../components/Icon.jsx';
import { ViewFreshnessNote } from '../../components/reports/ViewFreshnessNote.jsx';
import { Alert } from '../../components/ui/Alert.jsx';
import { Button } from '../../components/ui/Button.jsx';
import { DataTable } from '../../components/ui/DataTable.jsx';
import { Field, TextInput } from '../../components/ui/Field.jsx';
import { EmptyState, ErrorState, Loading, errorKind } from '../../components/ui/States.jsx';
import { useToast } from '../../components/ui/Toast.jsx';
import { explainError } from '../../lib/serverMessages.js';
import { formatDateTime, formatNumber, formatPercent } from '../../lib/format.js';

/** Flag that never relies on colour: icon + word. `umbral` is the threshold the value is compared with. */
function PctFlag({ pct, umbral }) {
  if (pct === null || pct === undefined) return <span className="flag flag-none"><Icon name="info" size="sm" />Sin entregas</span>;
  const low = Number(pct) < umbral;
  return low
    ? <span className="flag flag-low"><Icon name="alert" size="sm" />{formatPercent(pct)} · bajo el umbral</span>
    : <span className="flag flag-ok"><Icon name="check" size="sm" />{formatPercent(pct)} · sobre el umbral</span>;
}

/** Reportes: alerts below a threshold + full performance table, both from the materialized view. */
export function Reportes() {
  useDocumentTitle('Reportes');
  const toast = useToast();
  const [input, setInput] = useState('85');
  const [umbral, setUmbral] = useState(85);
  const [inputError, setInputError] = useState(null);
  const alertas = useAsync((s) => reportes.alertas(umbral, s), [umbral]);
  const rend = useAsync((s) => reportes.rendimiento(s), []);
  const [refreshing, setRefreshing] = useState(false);
  const [refreshed, setRefreshed] = useState(null);
  const [refreshError, setRefreshError] = useState(null);
  const refreshedRef = useRef(null);
  useEffect(() => { if (refreshed || refreshError) refreshedRef.current?.focus(); }, [refreshed, refreshError]);

  const applyThreshold = (e) => {
    e.preventDefault();
    const text = input.trim();
    const n = Number(text);
    if (!/^\d{1,3}$/.test(text) || n < 0 || n > 100) { setInputError('Ingresa un número entero entre 0 y 100.'); return; }
    setInputError(null);
    setUmbral(n);
  };

  const refresh = async () => {
    setRefreshing(true); setRefreshError(null);
    try {
      const r = await reportes.refrescar();
      setRefreshed(r.refrescadoEn);
      toast.success('Vista de rendimiento refrescada');
      alertas.reload(); rend.reload();
    } catch (err) {
      setRefreshError(explainError(err));
    } finally {
      setRefreshing(false);
    }
  };

  const repartidorCols = [
    { key: 'repartidorNombre', header: 'Repartidor', primary: true },
    { key: 'centroNombre', header: 'Centro' },
    { key: 'pct', header: '% a tiempo', render: (r) => <PctFlag pct={r.pctATiempo} umbral={alertas.data?.umbral ?? umbral} /> },
    { key: 'ent', header: 'Entregas a tiempo', numeric: true, mono: true, render: (r) => `${r.entregasATiempo} de ${r.entregasTotales}` },
    { key: 'intentosPromedio', header: 'Intentos promedio', numeric: true, mono: true, render: (r) => formatNumber(r.intentosPromedio) },
  ];
  const centroCols = [
    { key: 'centroNombre', header: 'Centro', primary: true },
    { key: 'pct', header: '% a tiempo', render: (r) => <PctFlag pct={r.pctATiempo} umbral={alertas.data?.umbral ?? umbral} /> },
    { key: 'ent', header: 'Entregas a tiempo', numeric: true, mono: true, render: (r) => `${r.entregasATiempo} de ${r.entregasTotales}` },
    { key: 'intentosPromedio', header: 'Intentos promedio', numeric: true, mono: true, render: (r) => formatNumber(r.intentosPromedio) },
  ];
  const rendCols = [
    { key: 'repartidorNombre', header: 'Repartidor', primary: true },
    { key: 'centroNombre', header: 'Centro' },
    { key: 'pct', header: '% a tiempo', render: (r) => <PctFlag pct={r.pctATiempo} umbral={alertas.data?.umbral ?? umbral} /> },
    { key: 'ent', header: 'Entregas a tiempo', numeric: true, mono: true, render: (r) => `${r.entregasATiempo} de ${r.entregasTotales}` },
    { key: 'paquetesAtendidos', header: 'Paquetes atendidos', numeric: true, mono: true },
    { key: 'intentosTotales', header: 'Intentos totales', numeric: true, mono: true },
    { key: 'intentosPromedio', header: 'Intentos promedio', numeric: true, mono: true, render: (r) => formatNumber(r.intentosPromedio) },
  ];

  const a = alertas.data;
  const total = a ? a.repartidores.length + a.centros.length : 0;

  return (
    <div className="page">
      <PageHeader
        title="Reportes"
        eyebrow="Despachador"
        actions={<Button variant="secondary" icon="refresh" onClick={refresh} loading={refreshing}>Refrescar vista</Button>}
      >
        Rendimiento de entregas a tiempo por repartidor y por centro.
      </PageHeader>

      <div ref={refreshedRef} tabIndex={-1} className="focus-target" role="status">
        {refreshed && <Alert tone="success" title={`Vista refrescada el ${formatDateTime(refreshed)}`} />}
        {refreshError && <Alert tone="danger" title="No se pudo refrescar la vista" detail={refreshError.detail}>{refreshError.message}</Alert>}
      </div>

      <ViewFreshnessNote>{refreshed ? ` Último refresco hecho desde esta sesión: ${formatDateTime(refreshed)}.` : ''}</ViewFreshnessNote>

      <section className="section" aria-labelledby="alertas-title">
        <div className="section-head">
          <h2 id="alertas-title">Alertas bajo el umbral</h2>
          <form className="threshold" onSubmit={applyThreshold} noValidate>
            <Field label="Umbral de % a tiempo (0 a 100)" error={inputError}>
              <TextInput inputMode="numeric" value={input} onChange={(e) => setInput(e.target.value)} />
            </Field>
            <Button type="submit" variant="secondary">Aplicar umbral</Button>
          </form>
        </div>

        {alertas.loading && !a && <Loading label="Cargando alertas" />}
        {alertas.error && <ErrorState kind={errorKind(alertas.error)} title="No pudimos cargar las alertas" message={alertas.error.message} action={{ label: 'Reintentar', onClick: alertas.reload }} />}
        {a && total === 0 && (
          <EmptyState icon="check" title={`Sin alertas con umbral ${a.umbral} %`} message="Ningún repartidor ni centro está bajo el umbral. Baja o sube el umbral para revisar otros casos." />
        )}
        {a && total > 0 && (
          <>
            <p role="status">
              Umbral {a.umbral} %: {a.repartidores.length} {a.repartidores.length === 1 ? 'repartidor' : 'repartidores'} y {a.centros.length} {a.centros.length === 1 ? 'centro' : 'centros'} por debajo.
            </p>
            {a.repartidores.length > 0 && (
              <DataTable dense caption="Repartidores bajo el umbral" columns={repartidorCols} rows={a.repartidores} rowKey={(r) => r.repartidorId} />
            )}
            {a.centros.length > 0 && (
              <DataTable dense caption="Centros bajo el umbral" columns={centroCols} rows={a.centros} rowKey={(r) => r.centroId} />
            )}
          </>
        )}
      </section>

      <section className="section" aria-labelledby="rend-title">
        <div className="section-head">
          <h2 id="rend-title">Rendimiento por repartidor</h2>
        </div>
        {rend.loading && !rend.data && <Loading label="Cargando rendimiento" />}
        {rend.error && <ErrorState kind={errorKind(rend.error)} title="No pudimos cargar el rendimiento" message={rend.error.message} action={{ label: 'Reintentar', onClick: rend.reload }} />}
        {rend.data && rend.data.length === 0 && (
          <EmptyState icon="chart" title="Aún no hay datos de rendimiento" message="Aparecerán cuando haya entregas resueltas y se refresque la vista." />
        )}
        {rend.data && rend.data.length > 0 && (
          <DataTable dense caption="Rendimiento por repartidor" columns={rendCols} rows={rend.data} rowKey={(r) => r.repartidorId} />
        )}
      </section>
    </div>
  );
}
