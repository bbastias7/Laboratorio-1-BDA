import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { paquetes } from '../../api';
import { useAsync } from '../../hooks/useAsync.js';
import { useDocumentTitle } from '../../hooks/useDocumentTitle.js';
import { PageHeader } from '../../components/layout/PageHeader.jsx';
import { Alert } from '../../components/ui/Alert.jsx';
import { Button, LinkButton } from '../../components/ui/Button.jsx';
import { Field, Textarea } from '../../components/ui/Field.jsx';
import { ErrorState, Loading, errorKind } from '../../components/ui/States.jsx';
import { PackageHeader } from '../../components/tracking/PackageHeader.jsx';
import { explainError } from '../../lib/serverMessages.js';
import { shortCode } from '../../lib/code.js';
import { formatNumber, formatKg, formatM3 } from '../../lib/format.js';

const FINAL = ['Entregado', 'Devuelto a origen'];

/** Detail of one own package with the destination change (PATCH /api/paquete/{id}/direccion). */
export function ClienteEnvio() {
  const { id } = useParams();
  const { data, error, loading, reload } = useAsync((s) => paquetes.getPaquete(id, s), [id]);
  useDocumentTitle(data ? `Envío ${shortCode(data.codigo)}` : 'Detalle de envío');

  return (
    <div className="page">
      <PageHeader title="Detalle del envío" eyebrow="Mis envíos" actions={<LinkButton to="/cliente" icon="list">Volver a mis envíos</LinkButton>} />
      {loading && !data && <Loading label="Cargando el envío" />}
      {error && (
        <ErrorState
          kind={errorKind(error)}
          title={error.kind === 'notFound' ? 'No encontramos este envío' : 'No pudimos cargar el envío'}
          message={error.kind === 'forbidden' ? 'Este envío pertenece a otra cuenta.' : error.message}
          action={error.kind === 'forbidden' || error.kind === 'notFound' ? { label: 'Ir a mis envíos', to: '/cliente' } : { label: 'Reintentar', onClick: reload }}
        />
      )}
      {data && <EnvioBody key={data.paqueteId} initial={data} />}
    </div>
  );
}

function EnvioBody({ initial }) {
  const [pkg, setPkg] = useState(initial);
  const [direccion, setDireccion] = useState(initial.direccionDestino);
  const [fieldError, setFieldError] = useState(null);
  const [serverError, setServerError] = useState(null);
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);
  const feedbackRef = useRef(null);
  const final = FINAL.includes(pkg.estado);

  useEffect(() => { if (serverError || saved) feedbackRef.current?.focus(); }, [serverError, saved]);

  const submit = async (e) => {
    e.preventDefault();
    setSaved(false);
    setServerError(null);
    const nueva = direccion.trim();
    if (!nueva) { setFieldError('Ingresa la nueva dirección de destino.'); return; }
    if (nueva.length > 255) { setFieldError('La dirección admite hasta 255 caracteres.'); return; }
    if (nueva === pkg.direccionDestino) { setFieldError('La dirección es igual a la actual. Escribe una distinta para cambiarla.'); return; }
    setFieldError(null);
    setBusy(true);
    try {
      const updated = await paquetes.cambiarDireccion(pkg.paqueteId, nueva);
      setPkg(updated);
      setDireccion(updated.direccionDestino);
      setSaved(true);
    } catch (err) {
      setServerError(explainError(err));
      // A conflict usually means the state changed after the page loaded: refresh the package to show the real state.
      if (err.kind === 'conflict') paquetes.getPaquete(pkg.paqueteId).then(setPkg).catch(() => {});
    } finally {
      setBusy(false);
    }
  };

  return (
    <>
      <PackageHeader tracking={pkg} />
      <p className="meta-line">
        <span>Peso <strong className="mono">{formatKg(pkg.peso)}</strong></span>
        <span>Volumen <strong className="mono">{formatM3(pkg.volumen)}</strong></span>
        <Link to={`/rastreo/${encodeURIComponent(pkg.codigo)}`}>Ver el seguimiento completo</Link>
      </p>

      <section className="section" aria-labelledby="destino-title">
        <div className="section-head">
          <h2 id="destino-title">Cambiar destino</h2>
        </div>

        {final ? (
          <Alert tone="warning" title={`No puedes cambiar el destino: el envío está ${pkg.estado}`}>
            <p>Los envíos {pkg.estado === 'Entregado' ? 'entregados' : 'devueltos a origen'} conservan su destino original para mantener el historial. Si necesitas enviar otro paquete, crea un envío nuevo.</p>
          </Alert>
        ) : (
          pkg.despachadoAt && (
            <Alert tone="info" title="Este envío ya fue despachado">
              <p>Puedes cambiar el destino, pero el cambio queda registrado en la auditoría con tu usuario y la fecha.</p>
            </Alert>
          )
        )}

        {saved && (
          <Alert ref={feedbackRef} tone="success" live="status" title="Destino actualizado">
            <p>El paquete se entregará en: {pkg.direccionDestino}</p>
          </Alert>
        )}
        {serverError && (
          <Alert ref={feedbackRef} tone="danger" title="No se pudo cambiar el destino" detail={serverError.detail}>
            <p>{serverError.message}</p>
          </Alert>
        )}

        <form className="form form-narrow" onSubmit={submit} noValidate>
          <Field label="Nueva dirección de destino" error={fieldError} hint={final ? undefined : 'Calle, número y comuna. Máximo 255 caracteres.'}>
            <Textarea rows={3} maxLength={255} value={direccion} disabled={final} onChange={(e) => { setDireccion(e.target.value); setFieldError(null); setSaved(false); }} />
          </Field>
          <div className="form-actions">
            <Button type="submit" variant="primary" loading={busy} disabled={final}>Guardar nuevo destino</Button>
          </div>
        </form>
      </section>
    </>
  );
}
