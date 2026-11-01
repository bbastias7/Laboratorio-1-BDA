import { forwardRef } from 'react';
import { Link } from 'react-router-dom';
import { Icon } from '../Icon.jsx';

/**
 * Button. Props: variant "primary" | "secondary" | "ghost" | "danger", size "md" (44 px) | "lg" (52 px, field use) | "sm" (36 px, dense tables),
 * icon (Icon name, optional), loading (disables and shows a status label), block (full width).
 * Defaults to type="button" so it never submits a form by accident.
 */
export const Button = forwardRef(function Button(
  { variant = 'primary', size = 'md', icon, loading = false, block = false, children, className = '', disabled, type = 'button', ...rest },
  ref,
) {
  const cls = ['btn', `btn-${variant}`, `btn-${size}`, block && 'btn-block', className].filter(Boolean).join(' ');
  return (
    <button ref={ref} type={type} className={cls} disabled={disabled || loading} aria-busy={loading || undefined} {...rest}>
      {icon && <Icon name={icon} />}
      <span>{loading ? 'Procesando…' : children}</span>
    </button>
  );
});

/** Same look as Button but renders a router <Link>. Props: `to`, variant, size, icon, block. */
export function LinkButton({ to, variant = 'secondary', size = 'md', icon, block = false, children, className = '', ...rest }) {
  const cls = ['btn', `btn-${variant}`, `btn-${size}`, block && 'btn-block', className].filter(Boolean).join(' ');
  return (
    <Link to={to} className={cls} {...rest}>
      {icon && <Icon name={icon} />}
      <span>{children}</span>
    </Link>
  );
}
