import { get, post } from './http.js';

/**
 * @typedef {{centroId:number, centroNombre:string, repartidorId:number, repartidorNombre:string, entregasTotales:number, entregasATiempo:number, intentosTotales:number, paquetesAtendidos:number, pctATiempo:number, intentosPromedio:number}} Rendimiento
 * @typedef {{centroId:number, centroNombre:string, entregasTotales:number, entregasATiempo:number, pctATiempo:number, intentosPromedio:number}} RendimientoCentro
 * @typedef {{umbral:number, repartidores:Rendimiento[], centros:RendimientoCentro[]}} Alertas
 */

/** All endpoints: despachador. Data comes from a materialized view: call refrescar() to recompute. */
export const rendimiento = (signal) => get('/api/reportes/rendimiento', undefined, signal);
/** GET /api/reportes/alertas?umbral=85. Couriers and centers whose on-time percentage is below the threshold. */
export const alertas = (umbral = 85, signal) => get('/api/reportes/alertas', { umbral }, signal);
/** POST /api/reportes/refrescar. Resolves to an object with the refresh timestamp (LocalDateTime string). */
export const refrescar = () => post('/api/reportes/refrescar');
