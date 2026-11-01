import { forwardRef } from 'react';
import { Icon } from '../Icon.jsx';

const ICON = { danger: 'alert', warning: 'alert', success: 'check', info: 'info' };

/**
 * Inline message tied to a form or a result. Tone is always carried by icon + text, never colour alone.
 * Props: tone "danger" | "warning" | "success" | "info", title (bold first line), children (explanation),
 * detail (original server text, shown small), live ("alert" | "status" | undefined). Focusable programmatically (tabIndex -1)
 * so a screen can move focus to it after a mutation.
 */
export const Alert = forwardRef(function Alert({ tone = 'info', title, children, detail, live, className = '' }, ref) {
  const role = live ?? (tone === 'danger' ? 'alert' : undefined);
  return (
    <div ref={ref} tabIndex={-1} role={role} className={`alert alert-${tone} ${className}`.trim()}>
      <Icon name={ICON[tone]} />
      <div className="alert-body">
        {title && <p className="alert-title">{title}</p>}
        {children && <div className="alert-text">{children}</div>}
        {detail && <p className="alert-detail">Mensaje del servidor: {detail}</p>}
      </div>
    </div>
  );
});
