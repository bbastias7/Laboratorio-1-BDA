import { useEffect } from 'react';

/** Sets the tab title as "<title> · Logística G5". */
export function useDocumentTitle(title) {
  useEffect(() => {
    document.title = title ? `${title} · Logística G5` : 'Logística G5 · Seguimiento de envíos';
  }, [title]);
}
