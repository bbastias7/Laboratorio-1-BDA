import { useEffect, useRef } from 'react';
import { Icon } from '../Icon.jsx';
import { Alert } from './Alert.jsx';
import { Button } from './Button.jsx';

/**
 * Modal built on the native <dialog>: focus is trapped and restored by the browser, Escape closes it.
 * Children are only mounted while open, so form state resets every time. Props: open, onClose, title, size "md" | "lg".
 */
export function Dialog({ open, onClose, title, size = 'md', children }) {
  const ref = useRef(null);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    if (open && !el.open) el.showModal();
    if (!open && el.open) el.close();
  }, [open]);

  return (
    <dialog ref={ref} className={`dialog dialog-${size}`} aria-labelledby="dialog-title" onClose={onClose}>
      {open && (
        <div className="dialog-inner">
          <header className="dialog-head">
            <h2 id="dialog-title">{title}</h2>
            <button type="button" className="icon-btn" onClick={onClose} aria-label="Cerrar ventana">
              <Icon name="close" />
            </button>
          </header>
          {children}
        </div>
      )}
    </dialog>
  );
}

/**
 * Confirmation step for an action that cannot be undone from the screen.
 * Props: open, title, children (what will happen), confirmLabel (verb + object), tone "primary" | "danger",
 * onConfirm (async; a thrown error is shown inside the dialog), onCancel, busy, error ({message, detail}).
 * Cancel comes first so the safe action receives the initial focus.
 */
export function ConfirmDialog({ open, title, children, confirmLabel, cancelLabel = 'Cancelar', tone = 'primary', onConfirm, onCancel, busy = false, error = null }) {
  return (
    <Dialog open={open} onClose={onCancel} title={title}>
      <div className="dialog-body">
        {error && <Alert tone="danger" title="No se pudo completar la acción" detail={error.detail}>{error.message}</Alert>}
        {children}
      </div>
      <footer className="dialog-actions">
        <Button variant="secondary" onClick={onCancel} disabled={busy}>{cancelLabel}</Button>
        <Button variant={tone === 'danger' ? 'danger' : 'primary'} onClick={onConfirm} loading={busy}>{confirmLabel}</Button>
      </footer>
    </Dialog>
  );
}
