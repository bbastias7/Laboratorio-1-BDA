# API client (`src/api/`)

Thin `fetch` wrappers, one function per backend endpoint. Paths, methods and field names come from the Java controllers, entities and `Dtos.java`.

```js
import { paquetes, tracking, ApiError } from '../api';
const lista = await paquetes.listPaquetes({ estado: 'En reparto' });
```

- `http.js` adds the Bearer token (refreshing it with `updateToken(30)`), sends JSON and parses the backend error body `{ "error": "texto" }` into an `ApiError` (`status`, `message`, `kind`).
- `kind`: `validation` (400), `unauthorized` (401), `forbidden` (403), `notFound` (404), `conflict` (409, business rule), `server` (5xx), `network` (0).
- 401: one forced token refresh and retry; a second 401 means "identity without local profile" and is thrown, not looped.
- 204 responses resolve to `null`. Dates are `LocalDateTime` strings without zone (`2026-11-05T18:00:00`), America/Santiago wall time: use `src/lib/format.js`.
- Every function accepts an `AbortSignal` as its last argument when it is a read.

## Function → endpoint → role

| Function | Endpoint | Role |
|---|---|---|
| `auth.me()` | GET `/api/auth/me` | any |
| `rastreo.rastrear(codigo)` | GET `/api/tracking/{codigo}` | any (cliente: own packages, else 403) |
| `paquetes.listPaquetes({estado,centroId})` | GET `/api/paquete` | any (scoped by role) |
| `paquetes.getPaquete(id)` | GET `/api/paquete/{id}` | any (scoped) |
| `paquetes.createPaquete(body)` | POST `/api/paquete` | cliente, despachador |
| `paquetes.updatePaquete(id, body)` | PUT `/api/paquete/{id}` | despachador |
| `paquetes.deletePaquete(id)` | DELETE `/api/paquete/{id}` | despachador |
| `paquetes.cambiarDireccion(id, dir)` | PATCH `/api/paquete/{id}/direccion` | cliente (own), despachador; 409 on final states |
| `paquetes.registrarEntregaFallida(id,{rutaId,motivo})` | POST `/api/paquete/{id}/entrega-fallida` | repartidor |
| `paquetes.marcarEntregado(id, rutaId)` | PATCH `/api/paquete/{id}/entregado` | repartidor |
| `entregas.listEntregas({soloPendientes})` | GET `/api/entregas` | repartidor |
| `rutas.listRutas()` / `getRuta(id)` | GET `/api/ruta`, `/api/ruta/{id}` | despachador, repartidor (own) |
| `rutas.asignarRuta(body)` | POST `/api/ruta/asignar` | despachador |
| `rutas.createRuta` / `updateRuta` / `deleteRuta` | POST/PUT/DELETE `/api/ruta[/{id}]` | despachador |
| `rutaPaquete.*` | `/api/rutapaquete[/{rutaId}/{paqueteId}]` | despachador |
| `centros.listCentros()` / `getCentro(id)` | GET `/api/centrodistribucion[/{id}]` | any |
| `centros.createCentro` / `updateCentro` / `deleteCentro` | POST/PUT/DELETE | despachador |
| `vehiculos.*` | `/api/vehiculos[/{id}]` | despachador |
| `repartidores.*` | `/api/repartidor[/{id}]` | despachador |
| `usuarios.*` (+ `findUsuarioByEmail`, `findUsuariosByNombre`, `listUsuariosByRol`) | `/api/usuarios...` | despachador |
| `eventos.*` | `/api/eventoseguimiento[/{id}]` | despachador |
| `auditoria.listAuditoria()` / `getAuditoria(id)` | GET `/api/auditoria[/{id}]` | despachador (read only) |
| `reportes.rendimiento()` | GET `/api/reportes/rendimiento` | despachador |
| `reportes.alertas(umbral=85)` | GET `/api/reportes/alertas` | despachador |
| `reportes.refrescar()` | POST `/api/reportes/refrescar` | despachador |

## Field names (exact JSON)

- Paquete: `paqueteId, usuarioClienteId, centroDistId, paqueteOrigenId, codigo, estado, peso, volumen, direccionDestino, intentos, fechaCompromiso, despachadoAt, entregadoAt, fechaReprogramada`
- Ruta: `rutaId, centroDistId, repartidorId, vehiculoId, fecha`
- RutaPaquete: `rutaId, paqueteId, resultado (Pendiente|Entregado|Fallido), fechaResultado`
- Centro: `centroDistId, nombre, direccion, ciudad` · Vehículo: `vehiculoId, centroDistId, patente, capacidadPeso, capacidadVolumen`
- Repartidor: `repartidorId, usuarioId, centroDistId` · Usuario: `usuarioId, nombre, email, rol, keycloakId`
- Evento: `eventoId, paqueteId, estado, descripcion, fecha` · Auditoría: `auditoriaId, paqueteId, usuarioId, direccionAnterior, direccionNueva, fecha`
- Estados de paquete: `Registrado, En centro, En reparto, Entregado, Reprogramado, Devuelto a origen`.
- Entrega (`/api/entregas`): `rutaId, fecha, paqueteId, codigo, direccionDestino, estado, resultado, intentos, fechaCompromiso`.
- Rendimiento: `centroId, centroNombre, repartidorId, repartidorNombre, entregasTotales, entregasATiempo, intentosTotales, paquetesAtendidos, pctATiempo, intentosPromedio`.

## Errors in the UI

Use `errorMessage(err)` for toasts and `err.kind` to branch: `forbidden` → `<ErrorState kind="forbidden" />`, `conflict` → show the message inline next to the form (it is a business-rule text from the database).
