import { get, post, put, del } from './http.js';

/** @typedef {{usuarioId:number, nombre:string, email:string, rol:'cliente'|'repartidor'|'despachador', keycloakId:string}} Usuario */

/** All endpoints: despachador. */
export const listUsuarios = (signal) => get('/api/usuarios', undefined, signal);
export const getUsuario = (id, signal) => get(`/api/usuarios/${id}`, undefined, signal);
/** GET /api/usuarios/email?correo= */
export const findUsuarioByEmail = (correo, signal) => get('/api/usuarios/email', { correo }, signal);
/** GET /api/usuarios/find?nombre= */
export const findUsuariosByNombre = (nombre, signal) => get('/api/usuarios/find', { nombre }, signal);
/** GET /api/usuarios/rol/{rol} */
export const listUsuariosByRol = (rol, signal) => get(`/api/usuarios/rol/${rol}`, undefined, signal);
export const createUsuario = (body) => post('/api/usuarios', body);
export const updateUsuario = (id, body) => put(`/api/usuarios/${id}`, body);
export const deleteUsuario = (id) => del(`/api/usuarios/${id}`);
