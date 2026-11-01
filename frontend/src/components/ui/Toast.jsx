import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react';
import { Icon } from '../Icon.jsx';

const ToastContext = createContext(null);
const ICON = { success: 'check', error: 'alert', info: 'info' };

/**
 * Status messages for mutations. `const toast = useToast(); toast.success('Paquete creado')`.
 * Success and info are announced politely (role="status"); errors assertively (role="alert") and stay until dismissed.
 */
export function ToastProvider({ children }) {
  const [items, setItems] = useState([]);
  const idRef = useRef(0);

  const dismiss = useCallback((id) => setItems((list) => list.filter((t) => t.id !== id)), []);
  const push = useCallback((tone, message) => {
    const id = ++idRef.current;
    setItems((list) => [...list, { id, tone, message }]);
    if (tone !== 'error') setTimeout(() => dismiss(id), 6000);
  }, [dismiss]);

  const api = useMemo(() => ({
    success: (m) => push('success', m),
    error: (m) => push('error', m),
    info: (m) => push('info', m),
  }), [push]);

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="toasts">
        {items.map((t) => (
          <div key={t.id} className={`toast toast-${t.tone}`} role={t.tone === 'error' ? 'alert' : 'status'}>
            <Icon name={ICON[t.tone]} />
            <p>{t.message}</p>
            <button type="button" className="toast-close" onClick={() => dismiss(t.id)} aria-label="Cerrar mensaje">
              <Icon name="close" size="sm" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used inside <ToastProvider>');
  return ctx;
}
