import { get, postPublic } from './http.js';

/** GET /api/auth/me. Any authenticated role: the SQL profile linked to the Keycloak identity (usuarioId, nombre, email, rol, keycloakId). */
export const me = (signal) => get('/api/auth/me', undefined, signal);

/**
 * POST /api/auth/registro. Public (no token). Creates a cliente account in Keycloak and the SQL profile.
 * 201 resolves to the profile; 409 = correo ya registrado; 400 = validation (message "campo: texto").
 */
export const registrar = ({ nombre, email, password }, signal) => postPublic('/api/auth/registro', { nombre, email, password }, signal);
