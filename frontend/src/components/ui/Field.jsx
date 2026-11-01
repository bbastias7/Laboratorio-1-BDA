import { cloneElement, isValidElement, useId } from 'react';

/**
 * Labelled form control wrapper. Visible label above the control, optional hint, error text below.
 * Props: label (string), hint, error, required, children (a single TextInput / Select / Textarea).
 * Wires id, aria-describedby and aria-invalid onto the child.
 */
export function Field({ label, hint, error, required = false, children, className = '' }) {
  const id = useId();
  const hintId = hint ? `${id}-hint` : undefined;
  const errId = error ? `${id}-err` : undefined;
  const describedBy = [hintId, errId].filter(Boolean).join(' ') || undefined;
  const control = isValidElement(children)
    ? cloneElement(children, { id, 'aria-describedby': describedBy, 'aria-invalid': error ? true : undefined, required: required || undefined })
    : children;
  return (
    <div className={`field ${error ? 'field-error' : ''} ${className}`.trim()}>
      <label htmlFor={id} className="field-label">
        {label}
        {required && <span className="field-required"> (obligatorio)</span>}
      </label>
      {hint && <p id={hintId} className="field-hint">{hint}</p>}
      {control}
      {error && <p id={errId} className="field-msg" role="alert">{error}</p>}
    </div>
  );
}

export function TextInput({ className = '', ...props }) {
  return <input className={`control ${className}`.trim()} {...props} />;
}

export function Select({ className = '', children, ...props }) {
  return <select className={`control control-select ${className}`.trim()} {...props}>{children}</select>;
}

export function Textarea({ className = '', rows = 3, ...props }) {
  return <textarea className={`control control-area ${className}`.trim()} rows={rows} {...props} />;
}
