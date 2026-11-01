import { get, post, put, del } from './http.js';

/** @typedef {{rutaId:number, centroDistId:number, repartidorId:number, vehiculoId:number, fecha:string}} Ruta */

/** GET /api/ruta. despachador (all) or repartidor (own). */
export const listRutas = (signal) => get('/api/ruta', undefined, signal);
/** GET /api/ruta/{id}. despachador or repartidor (own). */
export const getRuta = (id, signal) => get(`/api/ruta/${id}`, undefined, signal);
/** POST /api/ruta. despachador. Bare route without packages; prefer asignarRuta. */
export const createRuta = (body) => post('/api/ruta', body);
/**
 * POST /api/ruta/asignar. despachador. SP1: creates the route and assigns packages atomically.
 * @param {{centroDistId:number, repartidorId:number, vehiculoId:number, fecha?:string, paqueteIds:number[]}} body
 * 409 on capacity overflow or packages not assignable (state/center mismatch).
 */
export const asignarRuta = (body) => post('/api/ruta/asignar', body);
/** PUT /api/ruta/{id}. despachador. */
export const updateRuta = (id, body) => put(`/api/ruta/${id}`, body);
/** DELETE /api/ruta/{id}. despachador. */
export const deleteRuta = (id) => del(`/api/ruta/${id}`);
