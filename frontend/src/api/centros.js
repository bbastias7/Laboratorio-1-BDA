import { get, getPage, post, put, del } from './http.js';

/** @typedef {{centroDistId:number, nombre:string, direccion:string, ciudad:string}} CentroDistribucion */

/** GET list / by id: any authenticated role (cliente needs it to pick the origin center). Writes: despachador. */
export const listCentros = (signal) => get('/api/centrodistribucion', undefined, signal);
/** Paginated GET /api/centrodistribucion. Filters: ciudad, q (nombre). Resolves to { items, total }. */
export const pageCentros = (query, signal) => getPage('/api/centrodistribucion', query, signal);
export const getCentro = (id, signal) => get(`/api/centrodistribucion/${id}`, undefined, signal);
export const createCentro = (body) => post('/api/centrodistribucion', body);
export const updateCentro = (id, body) => put(`/api/centrodistribucion/${id}`, body);
export const deleteCentro = (id) => del(`/api/centrodistribucion/${id}`);
