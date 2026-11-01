import { get, post, put, patch, del } from './http.js';

/** @typedef {{paqueteId:number, usuarioClienteId:number, centroDistId:number, paqueteOrigenId:number|null, codigo:string, estado:string, peso:number, volumen:number, direccionDestino:string, intentos:number, fechaCompromiso:string, despachadoAt:string|null, entregadoAt:string|null, fechaReprogramada:string|null}} Paquete */

/** GET /api/paquete (cliente: own, repartidor: assigned, despachador: all). Filters apply server side. */
export const listPaquetes = ({ estado, centroId } = {}, signal) => get('/api/paquete', { estado, centroId }, signal);
/** GET /api/paquete/{id} */
export const getPaquete = (id, signal) => get(`/api/paquete/${id}`, undefined, signal);
/**
 * POST /api/paquete. cliente or despachador.
 * Required: centroDistId, peso (>0), volumen (>0), direccionDestino, fechaCompromiso (ISO local, "2026-11-05T18:00:00").
 * A cliente's usuarioClienteId is taken from the token; a despachador must send usuarioClienteId.
 * codigo, estado, intentos and the lifecycle dates are set by the server.
 */
export const createPaquete = (body) => post('/api/paquete', body);
/** PUT /api/paquete/{id}. despachador. Full entity. */
export const updatePaquete = (id, body) => put(`/api/paquete/${id}`, body);
/** DELETE /api/paquete/{id}. despachador. */
export const deletePaquete = (id) => del(`/api/paquete/${id}`);
/** PATCH /api/paquete/{id}/direccion. cliente (own) or despachador. 409 when the package is in a final state. */
export const cambiarDireccion = (id, direccionDestino) => patch(`/api/paquete/${id}/direccion`, { direccionDestino });
/** POST /api/paquete/{id}/entrega-fallida. repartidor (own route). SP2: reprograms or returns to origin. */
export const registrarEntregaFallida = (id, { rutaId, motivo }) => post(`/api/paquete/${id}/entrega-fallida`, { rutaId, motivo });
/** PATCH /api/paquete/{id}/entregado. repartidor (own route). */
export const marcarEntregado = (id, rutaId) => patch(`/api/paquete/${id}/entregado`, { rutaId });
