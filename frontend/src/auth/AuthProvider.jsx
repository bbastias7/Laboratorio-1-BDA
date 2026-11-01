import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { initKeycloak, keycloak, login as kcLogin, logout as kcLogout } from './keycloak.js';
import { pickRole, rolesFromToken } from './roles.js';
import { me } from '../api/auth.js';
import { errorMessage } from '../api/ApiError.js';

const AuthContext = createContext(null);

/**
 * status:
 *  - "loading"        Keycloak init or profile fetch in progress
 *  - "anonymous"      no session (public landing / tracking entry)
 *  - "authenticated"  session + role + profile available
 *  - "error"          session exists but the API rejected it or has no profile (show account-state screen)
 */
export function AuthProvider({ children }) {
  const [state, setState] = useState({ status: 'loading', user: null, error: null });

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const authenticated = await initKeycloak();
        if (cancelled) return;
        if (!authenticated) {
          setState({ status: 'anonymous', user: null, error: null });
          return;
        }
        const roles = rolesFromToken(keycloak.tokenParsed);
        const profile = await me();
        if (cancelled) return;
        // The DB profile decides the role when the token carries it (the backend enforces both match).
        const role = roles.includes(profile.rol) ? profile.rol : pickRole(roles);
        if (!role) {
          setState({ status: 'error', user: null, error: 'Tu cuenta no tiene un rol de Logística G5 asignado. Contacta a un despachador.' });
          return;
        }
        setState({ status: 'authenticated', error: null, user: { role, roles, profile, name: profile.nombre, email: profile.email } });
      } catch (err) {
        if (cancelled) return;
        setState({ status: 'error', user: null, error: errorMessage(err, 'No pudimos iniciar tu sesión.') });
      }
    })();
    return () => { cancelled = true; };
  }, []);

  const login = useCallback((path) => kcLogin(path), []);
  const logout = useCallback(() => kcLogout(), []);

  const value = useMemo(() => ({ ...state, login, logout }), [state, login, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/** { status, user: {role, roles, profile, name, email} | null, error, login(path), logout() } */
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
