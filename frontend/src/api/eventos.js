import { get, getPage, post, put, del } from './http.js';

/** @typedef {{eventoId:number, paqueteId:number, estado:'Registrado'|'En centro'|'En reparto'|'Entregado'|'Reprogramado'|'Devuelto a origen', descripcion:string, fecha:string}} EventoSeguimiento */

/** All endpoints: despachador. estado must be one of the six values; descripcion is required (max 255). */
export const listEventos = (signal) => get('/api/eventoseguimiento', undefined, signal);
/** Paginated GET /api/eventoseguimiento. Filters: paqueteId, estado. Resolves to { items, total }. */
export const pageEventos = (query, signal) => getPage('/api/eventoseguimiento', query, signal);
export const getEvento = (id, signal) => get(`/api/eventoseguimiento/${id}`, undefined, signal);
export const createEvento = (body) => post('/api/eventoseguimiento', body);
export const updateEvento = (id, body) => put(`/api/eventoseguimiento/${id}`, body);
export const deleteEvento = (id) => del(`/api/eventoseguimiento/${id}`);
