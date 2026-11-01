import { useEffect, useMemo, useRef, useState } from 'react';
import { entregas, paquetes } from '../../api';
import { useAuth } from '../../auth/AuthProvider.jsx';
import { useAsync } from '../../hooks/useAsync.js';
import { useDocumentTitle } from '../../hooks/useDocumentTitle.js';
import { PageHeader } from '../../components/layout/PageHeader.jsx';
import { Icon } from '../../components/Icon.jsx';
import { Alert } from '../../components/ui/Alert.jsx';
import { Button, LinkButton } from '../../components/ui/Button.jsx';
import { ConfirmDialog } from '../../components/ui/Dialog.jsx';
import { Field, Textarea } from '../../components/ui/Field.jsx';
import { StatusBadge } from '../../components/ui/StatusBadge.jsx';
import { EmptyState, ErrorState, Loading, errorKind } from '../../components/ui/States.jsx';
import { useToast } from '../../components/ui/Toast.jsx';
import { explainError } from '../../lib/serverMessages.js';
import { formatDate } from '../../lib/format.js';
import { shortCode } from '../../lib/code.js';
import { ShipmentCode } from '../../components/shipment/ShipmentCode.jsx';

// SP2 rule (registrar_entrega_fallida): attempts 1-3 reprogram the package, the 4th failure returns it to origin.
export const MAX_REPROGRAMMED_ATTEMPTS = 3;

export function ResultadoMark({ resultado }) {
  if (resultado === 'Entregado') return <span className="result-badge result-ok"><Icon name="check" size="sm" />Entregado</span>;
  if (resultado === 'Fallido') return <span className="result-badge result-fail"><Icon name="alert" size="sm" />Intento fallido</span>;
  return <span className="result-badge result-pending"><Icon name="clock" size="sm" />Pendiente</span>;
}

/** Mis entregas: the repartidor's deliveries grouped by route, with the two field actions. */
export function RepartidorHome() {
  const { user } = useAuth();
  const toast = useToast();
  useDocumentTitle('Mis entregas');
  const [soloPendientes, setSoloPendientes] = useState(true);
  const { data, error, loading, reload } = useAsync((s) => entregas.listEntregas({ soloPendientes }, s), [soloPendientes]);

  const [action, setAction] = useState(null); // { kind: 'entregado' | 'fallida', item }
  const [motivo, setMotivo] = useState('');
  const [busy, setBusy] = useState(false);
  const [dialogError, setDialogError] = useState(null);
  const [lastResult, setLastResult] = useState(null);
  const resultRef = useRef(null);
  const focusAfterLoad = useRef(false);
  // The acted delivery leaves the list when it reloads, so focus moves to the result message once the new list is in.
  useEffect(() => {
    if (focusAfterLoad.current && !loading) {
      focusAfterLoad.current = false;
      resultRef.current?.focus();
    }
  }, [data, loading]);

  const groups = useMemo(() => {
    const map = new Map();
    (data ?? []).forEach((e) => {
      if (!map.has(e.rutaId)) map.set(e.rutaId, { rutaId: e.rutaId, fecha: e.fecha, items: [] });
      map.get(e.rutaId).items.push(e);
    });
    return [...map.values()].sort((a, b) => b.rutaId - a.rutaId);
  }, [data]);

  const open = (kind, item) => { setAction({ kind, item }); setMotivo(''); setDialogError(null); };
  const close = () => { if (!busy) setAction(null); };

  const confirm = async () => {
    const { kind, item } = action;
    setBusy(true);
    setDialogError(null);
    try {
      if (kind === 'entregado') {
        await paquetes.marcarEntregado(item.paqueteId, item.rutaId);
        setLastResult({ tone: 'success', title: `${shortCode(item.codigo)} marcado como entregado`, text: 'La entrega quedó registrada y el cliente ya lo ve en su seguimiento.' });
        toast.success(`${shortCode(item.codigo)} entregado`);
      } else {
        const p = await paquetes.registrarEntregaFallida(item.paqueteId, { rutaId: item.rutaId, motivo: motivo.trim() || undefined });
        const returned = p.estado === 'Devuelto a origen';
        setLastResult({
          tone: 'warning',
          title: returned ? `${shortCode(item.codigo)}: devuelto a origen` : `${shortCode(item.codigo)}: intento fallido ${p.intentos} registrado`,
          text: returned
            ? 'Fue el cuarto intento fallido. El paquete vuelve al centro de origen y se creó un paquete de devolución.'
            : 'El paquete quedó Reprogramado. Un despachador deberá asignarlo a una nueva ruta.',
        });
        toast.info(returned ? `${shortCode(item.codigo)} devuelto a origen` : `Intento fallido de ${shortCode(item.codigo)} registrado`);
      }
      setAction(null);
      focusAfterLoad.current = true;
      reload();
    } catch (err) {
      setDialogError(explainError(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="page">
      <PageHeader
        title="Mis entregas"
        eyebrow={`Hola, ${user.name}`}
        actions={<LinkButton to="/repartidor/rutas" icon="route">Mis rutas</LinkButton>}
      />

      {lastResult && (
        <Alert ref={resultRef} tone={lastResult.tone} live="status" title={lastResult.title}>
          <p>{lastResult.text}</p>
        </Alert>
      )}

      <div className="toggle-row">
        <label className="check-field">
          <input type="checkbox" checked={soloPendientes} onChange={(e) => setSoloPendientes(e.target.checked)} />
          <span>Mostrar solo pendientes</span>
        </label>
        {data && <p className="sr-only" role="status">{data.length} entregas mostradas</p>}
      </div>

      {loading && !data && <Loading label="Cargando tus entregas" />}
      {error && (
        <ErrorState kind={errorKind(error)} title="No pudimos cargar tus entregas" message={error.message} action={{ label: 'Reintentar', onClick: reload }} />
      )}

      {data && groups.length === 0 && (
        <EmptyState
          icon="truck"
          title={soloPendientes ? 'No tienes entregas pendientes' : 'Aún no tienes entregas asignadas'}
          message={soloPendientes ? 'Cuando un despachador te asigne una ruta, tus paquetes aparecerán aquí.' : 'Cuando un despachador te asigne una ruta, aparecerá aquí con sus paquetes.'}
          action={soloPendientes ? { label: 'Ver también las ya resueltas', onClick: () => setSoloPendientes(false) } : undefined}
        />
      )}

      {groups.map((g) => (
        <section key={g.rutaId} className="route-group" aria-labelledby={`ruta-${g.rutaId}`}>
          <div className="route-group-head">
            <h2 id={`ruta-${g.rutaId}`}>Ruta {g.rutaId}</h2>
            <p>
              <span className="mono">{formatDate(g.fecha)}</span> · {g.items.length} {g.items.length === 1 ? 'entrega' : 'entregas'}
            </p>
          </div>
          <ul className="deliveries">
            {g.items.map((e) => (
              <li key={`${e.rutaId}-${e.paqueteId}`} className="delivery">
                <div className="delivery-top">
                  <span className="delivery-code"><ShipmentCode code={e.codigo} /></span>
                  <span className="pkg-state">
                    <ResultadoMark resultado={e.resultado} />
                    <StatusBadge estado={e.estado} />
                  </span>
                </div>
                <p className="delivery-dest">{e.direccionDestino}</p>
                <p className="delivery-meta">
                  <span>Intentos fallidos <strong className="mono">{e.intentos ?? 0}</strong></span>
                  <span>Compromiso <strong className="mono">{formatDate(e.fechaCompromiso)}</strong></span>
                </p>
                {e.resultado === 'Pendiente' && e.intentos >= MAX_REPROGRAMMED_ATTEMPTS && (
                  <Alert tone="warning" title="Último intento posible">
                    <p>Este paquete ya tuvo {e.intentos} intentos fallidos. Si falla otra vez, se devuelve al centro de origen.</p>
                  </Alert>
                )}
                {e.resultado === 'Pendiente' && (
                  <div className="delivery-actions">
                    <Button size="lg" variant="primary" icon="check" onClick={() => open('entregado', e)}>Marcar entregado</Button>
                    <Button size="lg" variant="secondary" icon="alert" onClick={() => open('fallida', e)}>Registrar entrega fallida</Button>
                  </div>
                )}
              </li>
            ))}
          </ul>
        </section>
      ))}

      <ConfirmDialog
        open={action?.kind === 'entregado'}
        title={`Marcar ${action ? shortCode(action.item.codigo) : ''} como entregado`}
        confirmLabel="Marcar entregado"
        onConfirm={confirm}
        onCancel={close}
        busy={busy}
        error={dialogError}
      >
        <p>Confirma que entregaste el paquete en <strong>{action?.item.direccionDestino}</strong>. Esta acción no se puede deshacer desde la app.</p>
      </ConfirmDialog>

      <ConfirmDialog
        open={action?.kind === 'fallida'}
        title={`Registrar entrega fallida de ${action ? shortCode(action.item.codigo) : ''}`}
        confirmLabel="Registrar entrega fallida"
        tone="danger"
        onConfirm={confirm}
        onCancel={close}
        busy={busy}
        error={dialogError}
      >
        {action?.kind === 'fallida' && (
          <>
            {(action.item.intentos ?? 0) + 1 > MAX_REPROGRAMMED_ATTEMPTS ? (
              <Alert tone="danger" title="Este sería el cuarto intento fallido">
                <p>El paquete se devolverá al centro de origen y se creará un paquete de devolución.</p>
              </Alert>
            ) : (
              <p>
                Será el intento fallido {(action.item.intentos ?? 0) + 1} de {MAX_REPROGRAMMED_ATTEMPTS}. El paquete quedará Reprogramado y un despachador deberá asignarlo a otra ruta.
                {(action.item.intentos ?? 0) + 1 === MAX_REPROGRAMMED_ATTEMPTS && ' Si vuelve a fallar después, se devuelve al centro de origen.'}
              </p>
            )}
            <Field label="Motivo (opcional)" hint="Por ejemplo: nadie en el domicilio. Máximo 100 caracteres.">
              <Textarea rows={2} maxLength={100} value={motivo} onChange={(e) => setMotivo(e.target.value)} />
            </Field>
          </>
        )}
      </ConfirmDialog>
    </div>
  );
}
