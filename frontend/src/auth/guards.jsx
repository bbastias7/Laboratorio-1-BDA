import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from './AuthProvider.jsx';
import { homeFor } from './roles.js';
import { Loading, ErrorState } from '../components/ui/States.jsx';

/** Waits for auth; anonymous users are sent to the landing with the intended path kept in router state. */
export function RequireAuth() {
  const { status } = useAuth();
  const location = useLocation();
  if (status === 'loading') return <Loading label="Verificando tu sesión" />;
  if (status === 'error') return <AccountProblem />;
  if (status === 'anonymous') return <Navigate to="/" replace state={{ from: location.pathname }} />;
  return <Outlet />;
}

/** Only lets `roles` through; any other signed-in role gets a 403 state with a link to their own home. */
export function RequireRole({ roles }) {
  const { user } = useAuth();
  if (!roles.includes(user.role)) {
    return (
      <ErrorState
        kind="forbidden"
        title="No tienes acceso a esta sección"
        message="Esta pantalla es para otro perfil de usuario."
        action={{ label: 'Ir a mi inicio', to: homeFor(user.role) }}
      />
    );
  }
  return <Outlet />;
}

export function AccountProblem() {
  const { error, logout } = useAuth();
  return (
    <ErrorState
      kind="forbidden"
      title="No pudimos validar tu cuenta"
      message={error ?? 'Tu sesión no es válida para Logística G5.'}
      action={{ label: 'Cerrar sesión', onClick: logout }}
    />
  );
}
