// Reference data for selects and for showing names instead of numeric ids.
// Each loader resolves to [{ value, label, raw }] where `value` is the id used by the API.
import { centros, paquetes, repartidores, rutas, usuarios, vehiculos } from '../../api';
import { formatDate } from '../../lib/format.js';

const opt = (value, label, raw) => ({ value, label, raw });

export const LOOKUPS = {
  centros: (s) => centros.listCentros(s).then((r) => r.map((c) => opt(c.centroDistId, `${c.nombre} (${c.ciudad})`, c))),
  usuarios: (s) => usuarios.listUsuarios(s).then((r) => r.map((u) => opt(u.usuarioId, `${u.nombre} · ${u.email}`, u))),
  clientes: (s) => usuarios.listUsuariosByRol('cliente', s).then((r) => r.map((u) => opt(u.usuarioId, `${u.nombre} · ${u.email}`, u))),
  usuariosRepartidor: (s) => usuarios.listUsuariosByRol('repartidor', s).then((r) => r.map((u) => opt(u.usuarioId, `${u.nombre} · ${u.email}`, u))),
  repartidores: async (s) => {
    const [reps, users] = await Promise.all([repartidores.listRepartidores(s), usuarios.listUsuarios(s)]);
    const name = Object.fromEntries(users.map((u) => [u.usuarioId, u.nombre]));
    return reps.map((r) => opt(r.repartidorId, `${name[r.usuarioId] ?? `Usuario ${r.usuarioId}`} (repartidor ${r.repartidorId})`, { ...r, nombre: name[r.usuarioId] }));
  },
  vehiculos: (s) => vehiculos.listVehiculos(s).then((r) => r.map((v) => opt(v.vehiculoId, v.patente, v))),
  rutas: (s) => rutas.listRutas(s).then((r) => r.map((x) => opt(x.rutaId, `Ruta ${x.rutaId} · ${formatDate(x.fecha)}`, x))),
  paquetes: (s) => paquetes.listPaquetes({}, s).then((r) => r.map((p) => opt(p.paqueteId, p.codigo ?? `Paquete ${p.paqueteId}`, p))),
};

export const loadLookups = async (keys, signal) => {
  const entries = await Promise.all(keys.map(async (k) => [k, await LOOKUPS[k](signal)]));
  return Object.fromEntries(entries);
};

/** Label for an id inside a loaded lookup set, falling back to the raw id. */
export const labelOf = (lookups, key, id) => lookups?.[key]?.find((o) => o.value === id)?.label ?? (id ?? '—');
