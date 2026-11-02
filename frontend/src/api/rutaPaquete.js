import { get, getPage, post, put, del } from './http.js';

/** @typedef {{rutaId:number, paqueteId:number, resultado:'Pendiente'|'Entregado'|'Fallido', fechaResultado:string|null}} RutaPaquete */

/** All endpoints: despachador. Composite key (rutaId, paqueteId). */
export const listRutaPaquetes = (signal) => get('/api/rutapaquete', undefined, signal);
/** Paginated GET /api/rutapaquete. Filters: rutaId, paqueteId, resultado. Resolves to { items, total }. */
export const pageRutaPaquetes = (query, signal) => getPage('/api/rutapaquete', query, signal);
export const getRutaPaquete = (rutaId, paqueteId, signal) => get(`/api/rutapaquete/${rutaId}/${paqueteId}`, undefined, signal);
export const createRutaPaquete = (body) => post('/api/rutapaquete', body);
export const updateRutaPaquete = (rutaId, paqueteId, body) => put(`/api/rutapaquete/${rutaId}/${paqueteId}`, body);
export const deleteRutaPaquete = (rutaId, paqueteId) => del(`/api/rutapaquete/${rutaId}/${paqueteId}`);
