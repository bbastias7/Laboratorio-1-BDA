import { get, post, put, del } from './http.js';

/** @typedef {{vehiculoId:number, centroDistId:number, patente:string, capacidadPeso:number, capacidadVolumen:number}} Vehiculo */

/** All endpoints: despachador. */
export const listVehiculos = (signal) => get('/api/vehiculos', undefined, signal);
export const getVehiculo = (id, signal) => get(`/api/vehiculos/${id}`, undefined, signal);
export const createVehiculo = (body) => post('/api/vehiculos', body);
export const updateVehiculo = (id, body) => put(`/api/vehiculos/${id}`, body);
export const deleteVehiculo = (id) => del(`/api/vehiculos/${id}`);
