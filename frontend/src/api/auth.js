import { get } from './http.js';

/** GET /api/auth/me. Any authenticated role: the SQL profile linked to the Keycloak identity (usuarioId, nombre, email, rol, keycloakId). */
export const me = (signal) => get('/api/auth/me', undefined, signal);
