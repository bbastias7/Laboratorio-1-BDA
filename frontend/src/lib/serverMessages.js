import { ApiError } from '../api/ApiError.js';

// Business-rule texts raised by the stored procedures and triggers are partly in English. This maps the known ones
// to Spanish for the UI and keeps the original text available as `detail`.
const c = (n) => String(Number(n)).replace('.', ',');
const RULES = [
  [/violates foreign key constraint .* on table "(\w+)"/, () => 'No se puede completar: otros registros dependen de este (por ejemplo rutas, paquetes o perfiles asociados). Elimina o reasigna primero los registros relacionados.'],
  [/violates foreign key constraint/, () => 'Algún valor elegido no existe o ya no está disponible. Actualiza la pantalla e inténtalo de nuevo.'],
  [/duplicate key value violates unique constraint/, () => 'Ya existe un registro con ese valor único (por ejemplo correo, patente o identificador).'],
  [/null value in column "(\w+)"/, (m) => `Falta un dato obligatorio (${m[1]}).`],
  [/Route (\d+) exceeds vehicle capacity \(weight ([\d.]+) \/ ([\d.]+), volume ([\d.]+) \/ ([\d.]+)\)/,
    (m) => `La carga excede la capacidad del vehículo: peso ${c(m[2])} de ${c(m[3])} kg y volumen ${c(m[4])} de ${c(m[5])} m³. No se asignó ningún paquete.`],
  [/Packages not assignable in their current state: (.+)/,
    (m) => `Hay paquetes que no se pueden asignar: su estado no lo permite o pertenecen a otro centro (ids ${m[1].replace(/[{}]/g, '')}). Actualiza la lista e inténtalo de nuevo.`],
  [/Packages already pending on a route: (.+)/,
    (m) => `Hay paquetes que ya están pendientes en otra ruta (ids ${m[1].replace(/[{}]/g, '')}).`],
  [/Courier \d+ does not belong to center \d+/, () => 'El repartidor elegido no pertenece al centro seleccionado.'],
  [/Vehicle \d+ does not belong to center \d+/, () => 'El vehículo elegido no pertenece al centro seleccionado.'],
  [/At least one package is required/, () => 'Selecciona al menos un paquete.'],
  [/Package list contains duplicate ids/, () => 'La lista de paquetes tiene ids repetidos.'],
  [/Packages not found: (.+)/, (m) => `No existen los paquetes con id ${m[1].replace(/[{}]/g, '')}.`],
  [/(Courier|Vehicle|Center) (\d+) not found/, (m) => `No se encontró ${({ Courier: 'el repartidor', Vehicle: 'el vehículo', Center: 'el centro' })[m[1]]} ${m[2]}.`],
  [/Package (\d+) is not assigned to route (\d+)/, (m) => `El paquete ${m[1]} no está asignado a la ruta ${m[2]}.`],
  [/Assignment of package (\d+) on route (\d+) is already (\w+)/, (m) => `La entrega del paquete ${m[1]} en la ruta ${m[2]} ya quedó resuelta (${m[3]}).`],
];

/**
 * Turns any thrown value into { message, detail }. `message` is what the UI shows (Spanish when the rule is known);
 * `detail` is the original server text when it differs, so nothing is hidden from the person debugging.
 */
export function explainError(err, fallback = 'Ocurrió un error inesperado. Inténtalo de nuevo.') {
  if (!(err instanceof ApiError) && !(err instanceof Error)) return { message: fallback, detail: null };
  const original = err.message || fallback;
  for (const [pattern, build] of RULES) {
    const m = pattern.exec(original);
    if (m) return { message: build(m), detail: original };
  }
  return { message: original, detail: null };
}
