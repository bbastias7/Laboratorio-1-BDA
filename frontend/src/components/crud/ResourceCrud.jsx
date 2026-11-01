import { useEffect, useMemo, useRef, useState } from 'react';
import { useAsync } from '../../hooks/useAsync.js';
import { useDocumentTitle } from '../../hooks/useDocumentTitle.js';
import { Alert } from '../ui/Alert.jsx';
import { Button } from '../ui/Button.jsx';
import { DataTable } from '../ui/DataTable.jsx';
import { ConfirmDialog, Dialog } from '../ui/Dialog.jsx';
import { Field, Select, TextInput, Textarea } from '../ui/Field.jsx';
import { EmptyState, ErrorState, Loading, errorKind } from '../ui/States.jsx';
import { useToast } from '../ui/Toast.jsx';
import { explainError } from '../../lib/serverMessages.js';
import { parseDecimal, positiveDecimalError } from '../../lib/numbers.js';
import { toApiDateTime } from '../../lib/format.js';
import { labelOf, loadLookups } from './lookups.js';

const PAGE_SIZE = 15;
const visibleIn = (field, mode) => !field.modes || field.modes.includes(mode);
const lower = (s) => s.charAt(0).toLowerCase() + s.slice(1);

/**
 * Config-driven CRUD screen. One component serves every resource; each resource only declares data (see resources.js):
 *
 *  {
 *    path, title, singular, fem?            naming (singular is lowercase, used in sentences)
 *    intro?                                 one sentence under the title
 *    getId(row)                             stable React key
 *    describe(row)                          short text naming a row in the delete confirmation
 *    deleteNote?                            what deleting a row implies (shown in the confirmation)
 *    api: { list(signal), create(body), update(row, body), remove(row) }
 *    lookups?: ['centros', ...]             reference lists loaded in parallel (see lookups.js)
 *    columns: [{ key, header, mono?, numeric?, primary?, render?(row, ctx), text?(row, ctx) }]
 *    fields:  [{ name, label, type, required?, max?, hint?, lookup?, options?, numeric?, modes?, readOnlyOn?, wide? }]
 *             type: 'text' | 'email' | 'decimal' | 'select' | 'textarea' | 'datetime-local'
 *    buildPayload?(values, { mode, row })   shape the request body (default: the field values)
 *    canEdit?(row), canDelete?(row)         per-row availability
 *    newestFirst?                           sort by descending id
 *  }
 *
 * ctx = { lookups, label(lookupKey, id) } is passed to render/text/options functions.
 */
export function ResourceCrud({ config }) {
  const toast = useToast();
  useDocumentTitle(config.title);
  const { data, error, loading, reload } = useAsync(async (signal) => {
    const [rows, lookups] = await Promise.all([config.api.list(signal), loadLookups(config.lookups ?? [], signal)]);
    return { rows, lookups };
  }, [config.path]);

  const ctx = useMemo(() => ({ lookups: data?.lookups ?? {}, label: (k, id) => labelOf(data?.lookups, k, id) }), [data]);
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(1);
  const [editing, setEditing] = useState(null); // { mode: 'create' | 'edit', row }
  const [deleting, setDeleting] = useState(null);
  const [deleteBusy, setDeleteBusy] = useState(false);
  const [deleteError, setDeleteError] = useState(null);
  const statusRef = useRef(null);
  const focusAfterLoad = useRef(false);
  const [statusText, setStatusText] = useState('');
  // The success message is held back until the list reload finishes, so the banner never describes a stale list.
  const [pendingText, setPendingText] = useState('');
  const reloadStarted = useRef(false);

  useEffect(() => {
    if (!pendingText) return;
    if (loading) { reloadStarted.current = true; return; }
    if (!reloadStarted.current) return; // the reload has not begun yet; wait for the loading phase
    reloadStarted.current = false;
    setStatusText(pendingText);
    toast.success(pendingText);
    setPendingText('');
    // After a delete the row (and its button) is gone once the list reloads: move focus to the status message then.
    if (focusAfterLoad.current) {
      focusAfterLoad.current = false;
      requestAnimationFrame(() => statusRef.current?.focus());
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pendingText, loading]);

  const rows = useMemo(() => {
    const all = [...(data?.rows ?? [])];
    if (config.newestFirst) all.reverse();
    const q = query.trim().toLowerCase();
    if (!q) return all;
    return all.filter((row) => config.columns
      .map((c) => (c.text ? c.text(row, ctx) : String(row[c.key] ?? '')))
      .join(' ').toLowerCase().includes(q));
  }, [data, query, config, ctx]);

  const pages = Math.max(1, Math.ceil(rows.length / PAGE_SIZE));
  const current = Math.min(page, pages);
  const pageRows = rows.slice((current - 1) * PAGE_SIZE, current * PAGE_SIZE);

  const done = (verb) => {
    const word = config.fem ? verb.fem : verb.masc;
    const text = `${config.singular.charAt(0).toUpperCase()}${config.singular.slice(1)} ${word}`;
    setStatusText('');
    reloadStarted.current = false;
    setPendingText(text);
    reload();
  };

  const confirmDelete = async () => {
    setDeleteBusy(true);
    setDeleteError(null);
    try {
      await config.api.remove(deleting);
      setDeleting(null);
      focusAfterLoad.current = true;
      done({ masc: 'eliminado', fem: 'eliminada' });
    } catch (err) {
      setDeleteError(explainError(err));
    } finally {
      setDeleteBusy(false);
    }
  };

  const columns = config.columns.map((c) => ({ ...c, render: c.render ? (row) => c.render(row, ctx) : undefined }));
  const hasActions = (row) => (config.canEdit?.(row) ?? true) || (config.canDelete?.(row) ?? true);

  return (
    <section className="section" aria-labelledby="crud-title">
      <div className="section-head">
        <div>
          <h2 id="crud-title">{config.title}</h2>
          {config.intro && <p>{config.intro}</p>}
        </div>
        <Button variant="primary" icon="plus" onClick={() => setEditing({ mode: 'create', row: null })} disabled={!data}>
          Crear {config.singular}
        </Button>
      </div>

      <div ref={statusRef} tabIndex={-1} className="focus-target" role="status" aria-live="polite">
        {statusText && <Alert tone="success" title={statusText} />}
      </div>

      {loading && !data && <Loading label={`Cargando ${lower(config.title)}`} />}
      {error && (
        <ErrorState kind={errorKind(error)} title={`No pudimos cargar ${lower(config.title)}`} message={error.message} action={{ label: 'Reintentar', onClick: reload }} />
      )}

      {data && (
        <>
          <div className="toolbar">
            <Field label={`Buscar en ${lower(config.title)}`}>
              <TextInput type="search" value={query} onChange={(e) => { setQuery(e.target.value); setPage(1); }} autoComplete="off" />
            </Field>
            <p className="pager" role="status">{rows.length} {rows.length === 1 ? 'registro' : 'registros'}</p>
          </div>

          {rows.length === 0 ? (
            data.rows.length === 0 ? (
              <EmptyState icon="list" title={`Aún no hay ${lower(config.title)}`} message={`Crea el primer registro con el botón «Crear ${config.singular}».`} />
            ) : (
              <EmptyState icon="search" title="Sin resultados" message="Ningún registro coincide con la búsqueda." action={{ label: 'Quitar búsqueda', onClick: () => setQuery('') }} />
            )
          ) : (
            <DataTable
              dense
              caption={config.title}
              columns={columns}
              rows={pageRows}
              rowKey={config.getId}
              rowActions={(row) => hasActions(row) && (
                <span className="row-actions">
                  {(config.canEdit?.(row) ?? true) && (
                    <Button size="sm" variant="secondary" icon="edit" onClick={() => setEditing({ mode: 'edit', row })} aria-label={`Editar ${config.describe(row)}`}>Editar</Button>
                  )}
                  {(config.canDelete?.(row) ?? true) && (
                    <Button size="sm" variant="ghost" icon="trash" onClick={() => { setDeleting(row); setDeleteError(null); }} aria-label={`Eliminar ${config.describe(row)}`}>Eliminar</Button>
                  )}
                </span>
              )}
            />
          )}

          {pages > 1 && (
            <nav className="pager" aria-label="Paginación">
              <Button size="sm" variant="secondary" onClick={() => setPage(current - 1)} disabled={current <= 1}>Anterior</Button>
              <span>Página {current} de {pages}</span>
              <Button size="sm" variant="secondary" onClick={() => setPage(current + 1)} disabled={current >= pages}>Siguiente</Button>
            </nav>
          )}
        </>
      )}

      <Dialog
        open={!!editing}
        onClose={() => setEditing(null)}
        title={editing?.mode === 'edit' ? `Editar ${config.singular}` : `Crear ${config.singular}`}
        size="lg"
      >
        {editing && (
          <ResourceForm
            config={config}
            mode={editing.mode}
            row={editing.row}
            ctx={ctx}
            onCancel={() => setEditing(null)}
            onSaved={() => { setEditing(null); done(editing.mode === 'edit' ? { masc: 'actualizado', fem: 'actualizada' } : { masc: 'creado', fem: 'creada' }); }}
          />
        )}
      </Dialog>

      <ConfirmDialog
        open={!!deleting}
        title={`Eliminar ${config.singular}`}
        confirmLabel={`Eliminar ${config.singular}`}
        tone="danger"
        onConfirm={confirmDelete}
        onCancel={() => !deleteBusy && setDeleting(null)}
        busy={deleteBusy}
        error={deleteError}
      >
        {deleting && (
          <>
            <p>Vas a eliminar <strong>{config.describe(deleting)}</strong>. Esta acción no se puede deshacer.</p>
            {config.deleteNote && <p>{config.deleteNote}</p>}
          </>
        )}
      </ConfirmDialog>
    </section>
  );
}

function initialValues(fields, row, mode) {
  const v = {};
  fields.filter((f) => visibleIn(f, mode)).forEach((f) => {
    const raw = row?.[f.name];
    if (f.type === 'decimal') v[f.name] = raw == null ? '' : String(raw).replace('.', ',');
    else if (f.type === 'datetime-local') v[f.name] = raw ? String(raw).slice(0, 16) : '';
    else v[f.name] = raw == null ? ((typeof f.defaultValue === 'function' ? f.defaultValue() : f.defaultValue) ?? '') : String(raw);
  });
  return v;
}

function fieldOptions(f, values, row, ctx) {
  if (typeof f.options === 'function') return f.options(values, row, ctx);
  if (f.options) return f.options;
  return ctx.lookups[f.lookup] ?? [];
}

function validateValues(fields, values, mode, row, ctx) {
  const errors = {};
  fields.filter((f) => visibleIn(f, mode) && !f.readOnlyOn?.includes(mode)).forEach((f) => {
    const raw = values[f.name] ?? '';
    const text = String(raw).trim();
    const label = f.noun ?? lower(f.label);
    if (f.type === 'decimal') {
      const m = positiveDecimalError(raw, label);
      if (m) errors[f.name] = m;
      return;
    }
    if (!text) {
      if (f.required) errors[f.name] = f.type === 'select' ? `Elige ${label}.` : `Ingresa ${label}.`;
      return;
    }
    if (f.max && text.length > f.max) errors[f.name] = `Admite hasta ${f.max} caracteres.`;
    else if (f.type === 'email' && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(text)) errors[f.name] = 'Ingresa un correo válido, por ejemplo nombre@dominio.cl.';
    else if (f.type === 'select' && !fieldOptions(f, values, row, ctx).some((o) => String(o.value) === text)) errors[f.name] = `Elige ${label} de la lista.`;
  });
  return errors;
}

function toPayload(fields, values, mode) {
  const body = {};
  fields.filter((f) => visibleIn(f, mode) && !f.readOnlyOn?.includes(mode)).forEach((f) => {
    const text = String(values[f.name] ?? '').trim();
    if (!text) return;
    if (f.type === 'decimal') body[f.name] = parseDecimal(text);
    else if (f.type === 'datetime-local') body[f.name] = toApiDateTime(text);
    else if (f.type === 'select' && f.numeric) body[f.name] = Number(text);
    else body[f.name] = text;
  });
  return body;
}

function ResourceForm({ config, mode, row, ctx, onCancel, onSaved }) {
  const fields = config.fields.filter((f) => visibleIn(f, mode));
  const [values, setValues] = useState(() => initialValues(config.fields, row, mode));
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState(null);
  const [busy, setBusy] = useState(false);
  const errorRef = useRef(null);
  useEffect(() => { if (serverError) errorRef.current?.focus(); }, [serverError]);

  const set = (name) => (e) => {
    setValues((v) => ({ ...v, [name]: e.target.value }));
    setErrors((er) => ({ ...er, [name]: undefined }));
  };

  const submit = async (e) => {
    e.preventDefault();
    setServerError(null);
    const found = validateValues(config.fields, values, mode, row, ctx);
    setErrors(found);
    if (Object.keys(found).length) {
      requestAnimationFrame(() => document.querySelector('dialog[open] [aria-invalid="true"]')?.focus());
      return;
    }
    setBusy(true);
    try {
      const base = toPayload(config.fields, values, mode);
      const body = config.buildPayload ? config.buildPayload(base, { mode, row }) : base;
      if (mode === 'edit') await config.api.update(row, body);
      else await config.api.create(body);
      onSaved();
    } catch (err) {
      const explained = explainError(err);
      const m = /^(\w+): (.+)$/.exec(explained.message);
      if (m && fields.some((f) => f.name === m[1])) {
        setErrors({ [m[1]]: m[2] });
        requestAnimationFrame(() => document.querySelector('dialog[open] [aria-invalid="true"]')?.focus());
      } else {
        setServerError(explained);
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="form" onSubmit={submit} noValidate>
      {config.formNote && mode === 'edit' && <Alert tone="info">{config.formNote}</Alert>}
      {serverError && (
        <Alert ref={errorRef} tone="danger" title="No se pudo guardar" detail={serverError.detail}>
          {serverError.message}
        </Alert>
      )}
      <div className="form-grid">
        {fields.map((f) => {
          const disabled = f.readOnlyOn?.includes(mode);
          const common = { value: values[f.name] ?? '', onChange: set(f.name), disabled };
          let control;
          if (f.type === 'select') {
            control = (
              <Select {...common}>
                <option value="">Elige una opción</option>
                {fieldOptions(f, values, row, ctx).map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
              </Select>
            );
          } else if (f.type === 'textarea') {
            control = <Textarea {...common} maxLength={f.max} />;
          } else if (f.type === 'decimal') {
            control = <TextInput {...common} inputMode="decimal" autoComplete="off" />;
          } else {
            control = <TextInput {...common} type={f.type === 'email' ? 'email' : f.type} maxLength={f.max} autoComplete="off" />;
          }
          return (
            <Field key={f.name} label={f.label} required={f.required && !disabled} error={errors[f.name]} hint={f.hint} className={f.wide ? 'field-wide' : ''}>
              {control}
            </Field>
          );
        })}
      </div>
      <div className="dialog-actions">
        <Button variant="secondary" onClick={onCancel} disabled={busy}>Cancelar</Button>
        <Button type="submit" variant="primary" loading={busy}>{mode === 'edit' ? 'Guardar cambios' : `Crear ${config.singular}`}</Button>
      </div>
    </form>
  );
}
