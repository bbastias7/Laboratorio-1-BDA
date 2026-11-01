import { useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { centros, paquetes, repartidores, rutaPaquete, rutas, usuarios, vehiculos } from '../../api';
import { useAsync } from '../../hooks/useAsync.js';
import { useDocumentTitle } from '../../hooks/useDocumentTitle.js';
import { PageHeader } from '../../components/layout/PageHeader.jsx';
import { Icon } from '../../components/Icon.jsx';
import { Alert } from '../../components/ui/Alert.jsx';
import { Button, LinkButton } from '../../components/ui/Button.jsx';
import { DataTable } from '../../components/ui/DataTable.jsx';
import { Field, Select, TextInput } from '../../components/ui/Field.jsx';
import { StatusBadge } from '../../components/ui/StatusBadge.jsx';
import { EmptyState, ErrorState, Loading, errorKind } from '../../components/ui/States.jsx';
import { useToast } from '../../components/ui/Toast.jsx';
import { explainError } from '../../lib/serverMessages.js';
import { formatDate, formatDateTime, formatNumber, toApiDateTime, formatKg, formatM3 } from '../../lib/format.js';
import { shortCode } from '../../lib/code.js';
import { ShipmentCode } from '../../components/shipment/ShipmentCode.jsx';

// Same states accepted by the asignar_ruta stored procedure.
const ELEGIBLES = ['Registrado', 'En centro', 'Reprogramado'];

function Meter({ label, unit, used, cap }) {
  const pct = cap > 0 ? Math.min(100, (used / cap) * 100) : 0;
  const over = used > cap;
  const near = !over && pct >= 90;
  return (
    <div className="meter-block">
      <div className="meter-line">
        <span>{label}</span>
        <strong>{formatNumber(used)} / {cap ? formatNumber(cap) : '—'} {unit}</strong>
      </div>
      <div className={`bar ${over ? 'bar-over' : near ? 'bar-warn' : ''}`} aria-hidden="true"><span style={{ '--pct': pct }} /></div>
      {over && <span className="meter-flag over"><Icon name="alert" size="sm" />Excede {formatNumber(used - cap)} {unit}</span>}
      {near && <span className="meter-flag near"><Icon name="alert" size="sm" />Cerca del límite</span>}
    </div>
  );
}

/** Asignar ruta (SP1): POST /api/ruta/asignar creates the route and assigns the packages atomically. */
export function AsignarRuta() {
  useDocumentTitle('Asignar ruta');
  const toast = useToast();
  const { data, error, loading, reload } = useAsync(async (signal) => {
    const [cds, reps, users, vehs, pks, asign] = await Promise.all([
      centros.listCentros(signal), repartidores.listRepartidores(signal), usuarios.listUsuarios(signal),
      vehiculos.listVehiculos(signal), paquetes.listPaquetes({}, signal), rutaPaquete.listRutaPaquetes(signal),
    ]);
    const nombres = Object.fromEntries(users.map((u) => [u.usuarioId, u.nombre]));
    return {
      centros: cds,
      repartidores: reps.map((r) => ({ ...r, nombre: nombres[r.usuarioId] ?? `Usuario ${r.usuarioId}` })),
      vehiculos: vehs,
      paquetes: pks,
      pendientes: new Set(asign.filter((a) => a.resultado === 'Pendiente').map((a) => a.paqueteId)),
    };
  }, []);

  const [centroId, setCentroId] = useState('');
  const [repartidorId, setRepartidorId] = useState('');
  const [vehiculoId, setVehiculoId] = useState('');
  const [fecha, setFecha] = useState('');
  const [selected, setSelected] = useState(() => new Set());
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState(null);
  const [busy, setBusy] = useState(false);
  const [created, setCreated] = useState(null);
  const errorRef = useRef(null);
  const resultRef = useRef(null);
  useEffect(() => { if (serverError) errorRef.current?.focus(); }, [serverError]);
  useEffect(() => { if (created) resultRef.current?.focus(); }, [created]);

  const reps = useMemo(() => (data?.repartidores ?? []).filter((r) => String(r.centroDistId) === centroId), [data, centroId]);
  const vehs = useMemo(() => (data?.vehiculos ?? []).filter((v) => String(v.centroDistId) === centroId), [data, centroId]);
  const elegibles = useMemo(
    () => (data?.paquetes ?? []).filter((p) => String(p.centroDistId) === centroId && ELEGIBLES.includes(p.estado) && !data.pendientes.has(p.paqueteId)),
    [data, centroId],
  );
  const vehiculo = vehs.find((v) => String(v.vehiculoId) === vehiculoId);
  const chosen = elegibles.filter((p) => selected.has(p.paqueteId));
  const peso = chosen.reduce((s, p) => s + Number(p.peso), 0);
  const volumen = chosen.reduce((s, p) => s + Number(p.volumen), 0);
  const exceeds = vehiculo && (peso > Number(vehiculo.capacidadPeso) || volumen > Number(vehiculo.capacidadVolumen));

  const changeCentro = (e) => {
    setCentroId(e.target.value); setRepartidorId(''); setVehiculoId(''); setSelected(new Set()); setErrors({}); setServerError(null);
  };
  const toggle = (id) => {
    setSelected((s) => { const n = new Set(s); if (n.has(id)) n.delete(id); else n.add(id); return n; });
    setServerError(null);
  };

  const submit = async (e) => {
    e.preventDefault();
    setServerError(null);
    const found = {};
    if (!centroId) found.centro = 'Elige el centro de distribución.';
    if (!repartidorId) found.repartidor = 'Elige un repartidor del centro.';
    if (!vehiculoId) found.vehiculo = 'Elige un vehículo del centro.';
    if (selected.size === 0) found.paquetes = 'Selecciona al menos un paquete.';
    setErrors(found);
    if (Object.keys(found).length) {
      requestAnimationFrame(() => document.querySelector('.assign [aria-invalid="true"]')?.focus());
      return;
    }
    setBusy(true);
    try {
      const ruta = await rutas.asignarRuta({
        centroDistId: Number(centroId), repartidorId: Number(repartidorId), vehiculoId: Number(vehiculoId),
        fecha: fecha ? toApiDateTime(fecha) : undefined,
        paqueteIds: [...selected],
      });
      setCreated({
        ruta, paquetes: chosen, peso, volumen,
        centro: data.centros.find((c) => c.centroDistId === ruta.centroDistId)?.nombre,
        repartidor: data.repartidores.find((r) => r.repartidorId === ruta.repartidorId)?.nombre,
        vehiculo: vehiculo?.patente,
      });
      toast.success(`Ruta ${ruta.rutaId} creada`);
    } catch (err) {
      setServerError(explainError(err));
    } finally {
      setBusy(false);
    }
  };

  const again = () => {
    setCreated(null); setSelected(new Set()); setServerError(null); setFecha(''); setErrors({});
    setRepartidorId(''); setVehiculoId('');
    reload();
  };

  const columns = [
    {
      key: 'sel', header: 'Seleccionar', hideHeader: true,
      render: (p) => (
        <label className="check-cell">
          <input type="checkbox" checked={selected.has(p.paqueteId)} onChange={() => toggle(p.paqueteId)} aria-label={`Seleccionar ${shortCode(p.codigo)}`} />
        </label>
      ),
    },
    { key: 'codigo', header: 'Código', mono: true, primary: true, render: (p) => <ShipmentCode code={p.codigo} /> },
    { key: 'estado', header: 'Estado', render: (p) => <StatusBadge estado={p.estado} /> },
    { key: 'direccionDestino', header: 'Destino' },
    { key: 'peso', header: 'Peso', numeric: true, mono: true, render: (p) => `${formatKg(p.peso)}` },
    { key: 'volumen', header: 'Volumen', numeric: true, mono: true, render: (p) => `${formatM3(p.volumen)}` },
    { key: 'fechaCompromiso', header: 'Compromiso', mono: true, render: (p) => formatDate(p.fechaCompromiso) },
  ];

  if (created) {
    return (
      <div className="page">
        <PageHeader title="Ruta creada" eyebrow="Asignar ruta" />
        <section className="created" aria-labelledby="ruta-creada">
          <div ref={resultRef} tabIndex={-1} className="focus-target" aria-live="polite">
            <h2 id="ruta-creada">Ruta {created.ruta.rutaId}</h2>
            <p className="meta-line">
              <span>Centro <strong>{created.centro}</strong></span>
              <span>Repartidor <strong>{created.repartidor}</strong></span>
              <span>Vehículo <strong className="mono">{created.vehiculo}</strong></span>
              <span>Fecha <strong className="mono">{formatDateTime(created.ruta.fecha)}</strong></span>
            </p>
          </div>
          <Alert tone="success" title={`${created.paquetes.length} ${created.paquetes.length === 1 ? 'paquete pasó' : 'paquetes pasaron'} a «En reparto»`}>
            <p>Carga total {formatKg(created.peso)} y {formatM3(created.volumen)}. El repartidor ya ve estas entregas.</p>
          </Alert>
          <ul className="assigned-list">
            {created.paquetes.map((p) => (
              <li key={p.paqueteId}><Link className="mono" to={`/rastreo/${encodeURIComponent(p.codigo)}`}><ShipmentCode code={p.codigo} /></Link><StatusBadge estado="En reparto" /></li>
            ))}
          </ul>
          <div className="form-actions">
            <Button variant="primary" onClick={again}>Asignar otra ruta</Button>
            <LinkButton to="/despachador/mantenedores/rutas">Ver rutas</LinkButton>
          </div>
        </section>
      </div>
    );
  }

  return (
    <div className="page">
      <PageHeader title="Asignar ruta" eyebrow="Despachador">
        Elige el centro, el repartidor y el vehículo, y marca los paquetes de la ruta. La capacidad del vehículo se valida en el servidor.
      </PageHeader>

      {loading && !data && <Loading label="Cargando centros, equipos y paquetes" />}
      {error && <ErrorState kind={errorKind(error)} title="No pudimos cargar los datos" message={error.message} action={{ label: 'Reintentar', onClick: reload }} />}

      {data && (
        <form className="assign" onSubmit={submit} noValidate>
          <div className="assign-main">
            {serverError && (
              <Alert ref={errorRef} tone="danger" title="El servidor rechazó la asignación" detail={serverError.detail}>
                <p>{serverError.message}</p>
                <p>No se creó la ruta ni cambió ningún paquete.</p>
              </Alert>
            )}

            <fieldset className="fieldset">
              <legend>1. Centro, repartidor y vehículo</legend>
              <div className="form-grid">
                <Field label="Centro de distribución" required error={errors.centro} className="field-wide">
                  <Select value={centroId} onChange={changeCentro}>
                    <option value="">Elige un centro</option>
                    {data.centros.map((c) => <option key={c.centroDistId} value={c.centroDistId}>{c.nombre} · {c.ciudad}</option>)}
                  </Select>
                </Field>
                <Field label="Repartidor" required error={errors.repartidor} hint={centroId && reps.length === 0 ? 'Este centro no tiene repartidores.' : 'Solo repartidores del centro.'}>
                  <Select value={repartidorId} onChange={(e) => setRepartidorId(e.target.value)} disabled={!centroId}>
                    <option value="">{centroId ? 'Elige un repartidor' : 'Elige primero un centro'}</option>
                    {reps.map((r) => <option key={r.repartidorId} value={r.repartidorId}>{r.nombre}</option>)}
                  </Select>
                </Field>
                <Field label="Vehículo" required error={errors.vehiculo} hint={centroId && vehs.length === 0 ? 'Este centro no tiene vehículos.' : 'Se muestra la capacidad de peso y volumen.'}>
                  <Select value={vehiculoId} onChange={(e) => setVehiculoId(e.target.value)} disabled={!centroId}>
                    <option value="">{centroId ? 'Elige un vehículo' : 'Elige primero un centro'}</option>
                    {vehs.map((v) => <option key={v.vehiculoId} value={v.vehiculoId}>{v.patente} · {formatKg(v.capacidadPeso)} · {formatM3(v.capacidadVolumen)}</option>)}
                  </Select>
                </Field>
                <Field label="Fecha y hora de la ruta (opcional)" hint="Si la dejas vacía se usa el momento actual.">
                  <TextInput type="datetime-local" value={fecha} onChange={(e) => setFecha(e.target.value)} />
                </Field>
              </div>
            </fieldset>

            <fieldset className="fieldset">
              <legend>2. Paquetes de la ruta</legend>
              {errors.paquetes && <p className="field-msg" role="alert">{errors.paquetes}</p>}
              {!centroId && <p className="notice">Elige un centro para ver sus paquetes disponibles.</p>}
              {centroId && elegibles.length === 0 && (
                <EmptyState icon="box" title="No hay paquetes disponibles en este centro" message="Solo se pueden asignar paquetes Registrado, En centro o Reprogramado que no estén ya pendientes en otra ruta." />
              )}
              {centroId && elegibles.length > 0 && (
                <>
                  <p className="notice">{elegibles.length} {elegibles.length === 1 ? 'paquete disponible' : 'paquetes disponibles'} (Registrado, En centro o Reprogramado, sin ruta pendiente).</p>
                  <DataTable dense caption="Paquetes disponibles para la ruta" columns={columns} rows={elegibles} rowKey={(p) => p.paqueteId} />
                </>
              )}
            </fieldset>
          </div>

          <aside className="summary" aria-labelledby="resumen-title">
            <h2 id="resumen-title">Resumen de la ruta</h2>
            <p aria-live="polite"><strong className="mono">{selected.size}</strong> {selected.size === 1 ? 'paquete seleccionado' : 'paquetes seleccionados'}</p>
            <Meter label="Peso" unit="kg" used={peso} cap={Number(vehiculo?.capacidadPeso ?? 0)} />
            <Meter label="Volumen" unit="m³" used={volumen} cap={Number(vehiculo?.capacidadVolumen ?? 0)} />
            {!vehiculo && <p className="field-hint">Elige un vehículo para comparar la carga con su capacidad.</p>}
            {exceeds && (
              <Alert tone="warning" live="status" title="La carga excede la capacidad">
                <p>Puedes enviar la asignación, pero el servidor la rechazará. Quita paquetes o elige otro vehículo.</p>
              </Alert>
            )}
            <Button type="submit" variant="primary" size="lg" icon="route" loading={busy}>Asignar ruta</Button>
          </aside>
        </form>
      )}
    </div>
  );
}
