import { get } from './http.js';

/** @typedef {{auditoriaId:number, paqueteId:number, usuarioId:number|null, direccionAnterior:string, direccionNueva:string, fecha:string}} AuditoriaDireccion */

/** Read only (the backend exposes no write endpoints). despachador. */
export const listAuditoria = (signal) => get('/api/auditoria', undefined, signal);
export const getAuditoria = (id, signal) => get(`/api/auditoria/${id}`, undefined, signal);
