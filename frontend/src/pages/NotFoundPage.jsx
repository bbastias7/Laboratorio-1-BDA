import { ErrorState } from '../components/ui/States.jsx';
import { useDocumentTitle } from '../hooks/useDocumentTitle.js';

export function NotFoundPage() {
  useDocumentTitle('Página no encontrada');
  return (
    <ErrorState
      kind="notFound"
      title="Esta página no existe"
      message="La dirección que abriste no corresponde a ninguna pantalla."
      action={{ label: 'Ir al inicio', to: '/' }}
    />
  );
}
