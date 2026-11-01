import { get } from './http.js';

/** @typedef {{rutaId:number, fecha:string, paqueteId:number, codigo:string, direccionDestino:string, estado:string, resultado:'Pendiente'|'Entregado'|'Fallido', intentos:number, fechaCompromiso:string}} EntregaView */

/** GET /api/entregas. repartidor only: the caller's own deliveries. `soloPendientes` hides finished ones. */
export const listEntregas = ({ soloPendientes = false } = {}, signal) =>
  get('/api/entregas', { soloPendientes }, signal);
