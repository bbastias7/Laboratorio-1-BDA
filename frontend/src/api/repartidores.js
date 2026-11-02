import { get, getPage, post, put, del } from './http.js';

/** @typedef {{repartidorId:number, usuarioId:number, centroDistId:number}} Repartidor */

/** All endpoints: despachador. (The repartidor's own deliveries live in entregas.js.) */
export const listRepartidores = (signal) => get('/api/repartidor', undefined, signal);
/** Paginated GET /api/repartidor. Filters: centroId, usuarioId. Resolves to { items, total }. */
export const pageRepartidores = (query, signal) => getPage('/api/repartidor', query, signal);
export const getRepartidor = (id, signal) => get(`/api/repartidor/${id}`, undefined, signal);
export const createRepartidor = (body) => post('/api/repartidor', body);
export const updateRepartidor = (id, body) => put(`/api/repartidor/${id}`, body);
export const deleteRepartidor = (id) => del(`/api/repartidor/${id}`);
