import { get } from './http.js';

/**
 * @typedef {{estado:string, descripcion:string, fecha:string}} EventoView
 * @typedef {{codigo:string, estado:string, direccionDestino:string, fechaCompromiso:string, entregadoAt:string|null, intentos:number, fechaReprogramada:string|null, eventos:EventoView[]}} TrackingResponse
 */

/**
 * GET /api/tracking/{codigo}. Any authenticated role.
 * 404 unknown code; 403 when a cliente asks for someone else's package. `eventos` come ordered by date.
 * @returns {Promise<TrackingResponse>}
 */
export const rastrear = (codigo, signal) => get(`/api/tracking/${encodeURIComponent(codigo)}`, undefined, signal);
