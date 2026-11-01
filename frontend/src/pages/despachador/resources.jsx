// One entry per maintainer screen. Each config is plain data for <ResourceCrud />: exact DTO field names, validation
// limits taken from the Java entities (@Size, @Pattern, @DecimalMin) and the database triggers' business rules.
import { Link } from 'react-router-dom';
import { centros, eventos, paquetes, repartidores, rutaPaquete, rutas, usuarios, vehiculos } from '../../api';
import { StatusBadge, ESTADOS } from '../../components/ui/StatusBadge.jsx';
import { formatDate, formatDateTime, formatNumber, formatKg, formatM3 } from '../../lib/format.js';
import { shortCode } from '../../lib/code.js';
import { TrackingCode } from '../../components/tracking/TrackingCode.jsx';
import { nowLocalInput } from '../../lib/numbers.js';

const ESTADO_OPTIONS = Object.keys(ESTADOS).map((e) => ({ value: e, label: e }));
const ROL_OPTIONS = ['cliente', 'repartidor', 'despachador'].map((r) => ({ value: r, label: r }));
const ELEGIBLES = ['Registrado', 'En centro', 'Reprogramado'];

const nameOfUser = (ctx, id) => ctx.lookups.usuarios?.find((o) => o.value === id)?.raw.nombre ?? `Usuario ${id}`;
const byCentro = (key) => (values, _row, ctx) =>
  (ctx.lookups[key] ?? []).filter((o) => !values.centroDistId || String(o.raw.centroDistId) === String(values.centroDistId));

const usuariosConfig = {
  path: 'usuarios', title: 'Usuarios', singular: 'usuario',
  intro: 'Perfiles de la aplicación. Cada perfil se vincula a una cuenta de Keycloak por su identificador.',
  getId: (r) => r.usuarioId,
  describe: (r) => `${r.nombre} (${r.email})`,
  deleteNote: 'Si el usuario tiene paquetes, rutas o perfil de repartidor asociados, el servidor rechazará la eliminación.',
  formNote: 'La identidad de un perfil no se puede cambiar si ya tiene operaciones asociadas; el servidor lo rechazará.',
  api: {
    list: (s) => usuarios.listUsuarios(s),
    create: (b) => usuarios.createUsuario(b),
    update: (row, b) => usuarios.updateUsuario(row.usuarioId, b),
    remove: (row) => usuarios.deleteUsuario(row.usuarioId),
  },
  columns: [
    { key: 'usuarioId', header: 'Id', mono: true, numeric: true },
    { key: 'nombre', header: 'Nombre', primary: true },
    { key: 'email', header: 'Correo' },
    { key: 'rol', header: 'Rol' },
    { key: 'keycloakId', header: 'Id Keycloak', mono: true },
  ],
  fields: [
    { name: 'nombre', label: 'Nombre', type: 'text', required: true, max: 64 },
    { name: 'email', label: 'Correo electrónico', type: 'email', required: true, max: 128 },
    { name: 'rol', label: 'Rol', type: 'select', required: true, options: ROL_OPTIONS },
    { name: 'keycloakId', label: 'Identificador de Keycloak', type: 'text', required: true, max: 128, wide: true, hint: 'Valor «sub» de la cuenta en Keycloak (un UUID).' },
  ],
};

const centrosConfig = {
  path: 'centros', title: 'Centros de distribución', singular: 'centro de distribución',
  getId: (r) => r.centroDistId,
  describe: (r) => r.nombre,
  deleteNote: 'Si el centro tiene vehículos, repartidores, rutas o paquetes asociados, el servidor rechazará la eliminación.',
  api: {
    list: (s) => centros.listCentros(s),
    create: (b) => centros.createCentro(b),
    update: (row, b) => centros.updateCentro(row.centroDistId, b),
    remove: (row) => centros.deleteCentro(row.centroDistId),
  },
  columns: [
    { key: 'centroDistId', header: 'Id', mono: true, numeric: true },
    { key: 'nombre', header: 'Nombre', primary: true },
    { key: 'direccion', header: 'Dirección' },
    { key: 'ciudad', header: 'Ciudad' },
  ],
  fields: [
    { name: 'nombre', label: 'Nombre', type: 'text', required: true, max: 64 },
    { name: 'ciudad', label: 'Ciudad', type: 'text', required: true, max: 64 },
    { name: 'direccion', label: 'Dirección', type: 'text', required: true, max: 128, wide: true },
  ],
};

const vehiculosConfig = {
  path: 'vehiculos', title: 'Vehículos', singular: 'vehículo',
  intro: 'La capacidad de peso y volumen es el límite que valida la base de datos al asignar rutas.',
  getId: (r) => r.vehiculoId,
  describe: (r) => r.patente,
  deleteNote: 'Si el vehículo tiene rutas asociadas, el servidor rechazará la eliminación.',
  formNote: 'No puedes bajar la capacidad por debajo de la carga de una ruta existente, ni cambiar el centro de un vehículo con rutas.',
  lookups: ['centros'],
  api: {
    list: (s) => vehiculos.listVehiculos(s),
    create: (b) => vehiculos.createVehiculo(b),
    update: (row, b) => vehiculos.updateVehiculo(row.vehiculoId, b),
    remove: (row) => vehiculos.deleteVehiculo(row.vehiculoId),
  },
  columns: [
    { key: 'vehiculoId', header: 'Id', mono: true, numeric: true },
    { key: 'patente', header: 'Patente', mono: true, primary: true },
    { key: 'centroDistId', header: 'Centro', render: (r, ctx) => ctx.label('centros', r.centroDistId), text: (r, ctx) => String(ctx.label('centros', r.centroDistId)) },
    { key: 'capacidadPeso', header: 'Capacidad de peso', numeric: true, mono: true, render: (r) => `${formatKg(r.capacidadPeso)}` },
    { key: 'capacidadVolumen', header: 'Capacidad de volumen', numeric: true, mono: true, render: (r) => `${formatM3(r.capacidadVolumen)}` },
  ],
  fields: [
    { name: 'centroDistId', label: 'Centro de distribución', type: 'select', required: true, numeric: true, lookup: 'centros', wide: true },
    { name: 'patente', label: 'Patente', type: 'text', required: true, max: 16 },
    { name: 'capacidadPeso', label: 'Capacidad de peso (kg)', noun: 'la capacidad de peso en kilos', type: 'decimal' },
    { name: 'capacidadVolumen', label: 'Capacidad de volumen (m³)', noun: 'la capacidad de volumen en metros cúbicos', type: 'decimal' },
  ],
};

const repartidoresConfig = {
  path: 'repartidores', title: 'Repartidores', singular: 'repartidor',
  intro: 'Perfil operativo de un usuario con rol repartidor, asignado a un centro.',
  getId: (r) => r.repartidorId,
  describe: (r) => `repartidor ${r.repartidorId}`,
  deleteNote: 'Si el repartidor tiene rutas asociadas, el servidor rechazará la eliminación.',
  formNote: 'No se puede reasignar un repartidor que ya tiene rutas históricas.',
  lookups: ['usuarios', 'usuariosRepartidor', 'centros'],
  api: {
    list: (s) => repartidores.listRepartidores(s),
    create: (b) => repartidores.createRepartidor(b),
    update: (row, b) => repartidores.updateRepartidor(row.repartidorId, b),
    remove: (row) => repartidores.deleteRepartidor(row.repartidorId),
  },
  columns: [
    { key: 'repartidorId', header: 'Id', mono: true, numeric: true },
    { key: 'usuarioId', header: 'Usuario', primary: true, render: (r, ctx) => nameOfUser(ctx, r.usuarioId), text: (r, ctx) => nameOfUser(ctx, r.usuarioId) },
    { key: 'centroDistId', header: 'Centro', render: (r, ctx) => ctx.label('centros', r.centroDistId), text: (r, ctx) => String(ctx.label('centros', r.centroDistId)) },
  ],
  fields: [
    { name: 'usuarioId', label: 'Usuario', type: 'select', required: true, numeric: true, lookup: 'usuariosRepartidor', wide: true, hint: 'Solo aparecen usuarios con rol repartidor.' },
    { name: 'centroDistId', label: 'Centro de distribución', type: 'select', required: true, numeric: true, lookup: 'centros', wide: true },
  ],
};

const rutasConfig = {
  path: 'rutas', title: 'Rutas', singular: 'ruta', fem: true, newestFirst: true,
  intro: 'Para crear una ruta con paquetes y validar la capacidad usa «Asignar ruta».',
  getId: (r) => r.rutaId,
  describe: (r) => `la ruta ${r.rutaId}`,
  deleteNote: 'Si la ruta tiene paquetes asignados, el servidor rechazará la eliminación.',
  formNote: 'Una ruta que ya tiene paquetes asignados no se puede modificar; el servidor lo rechazará.',
  lookups: ['centros', 'repartidores', 'vehiculos'],
  api: {
    list: (s) => rutas.listRutas(s),
    create: (b) => rutas.createRuta(b),
    update: (row, b) => rutas.updateRuta(row.rutaId, b),
    remove: (row) => rutas.deleteRuta(row.rutaId),
  },
  columns: [
    { key: 'rutaId', header: 'Ruta', mono: true, numeric: true, primary: true },
    { key: 'fecha', header: 'Fecha', mono: true, render: (r) => formatDateTime(r.fecha), text: (r) => formatDateTime(r.fecha) },
    { key: 'centroDistId', header: 'Centro', render: (r, ctx) => ctx.label('centros', r.centroDistId), text: (r, ctx) => String(ctx.label('centros', r.centroDistId)) },
    { key: 'repartidorId', header: 'Repartidor', render: (r, ctx) => ctx.label('repartidores', r.repartidorId), text: (r, ctx) => String(ctx.label('repartidores', r.repartidorId)) },
    { key: 'vehiculoId', header: 'Vehículo', mono: true, render: (r, ctx) => ctx.label('vehiculos', r.vehiculoId), text: (r, ctx) => String(ctx.label('vehiculos', r.vehiculoId)) },
  ],
  fields: [
    { name: 'centroDistId', label: 'Centro de distribución', type: 'select', required: true, numeric: true, lookup: 'centros', wide: true },
    { name: 'repartidorId', label: 'Repartidor', type: 'select', required: true, numeric: true, options: byCentro('repartidores'), hint: 'Solo repartidores del centro elegido.' },
    { name: 'vehiculoId', label: 'Vehículo', type: 'select', required: true, numeric: true, options: byCentro('vehiculos'), hint: 'Solo vehículos del centro elegido.' },
    { name: 'fecha', label: 'Fecha y hora', type: 'datetime-local', defaultValue: nowLocalInput, wide: true, hint: 'Si la dejas vacía se usa el momento actual.' },
  ],
};

const rutaPaqueteConfig = {
  path: 'asignaciones', title: 'Asignaciones ruta-paquete', singular: 'asignación', fem: true, newestFirst: true,
  intro: 'Las asignaciones resueltas (Entregado o Fallido) son historial: no se editan ni se eliminan.',
  getId: (r) => `${r.rutaId}-${r.paqueteId}`,
  describe: (r) => `la asignación del paquete ${r.paqueteId} a la ruta ${r.rutaId}`,
  deleteNote: 'Al quitar un paquete pendiente de una ruta, vuelve al estado «En centro».',
  formNote: 'Solo puedes marcar el resultado como Entregado o Fallido; una asignación resuelta no se reabre. Marcarla Fallida cuenta como intento fallido.',
  lookups: ['rutas', 'paquetes'],
  canEdit: (r) => r.resultado === 'Pendiente',
  canDelete: (r) => r.resultado === 'Pendiente',
  api: {
    list: (s) => rutaPaquete.listRutaPaquetes(s),
    create: (b) => rutaPaquete.createRutaPaquete(b),
    update: (row, b) => rutaPaquete.updateRutaPaquete(row.rutaId, row.paqueteId, { rutaId: row.rutaId, paqueteId: row.paqueteId, resultado: b.resultado }),
    remove: (row) => rutaPaquete.deleteRutaPaquete(row.rutaId, row.paqueteId),
  },
  columns: [
    { key: 'rutaId', header: 'Ruta', render: (r, ctx) => ctx.label('rutas', r.rutaId), text: (r, ctx) => String(ctx.label('rutas', r.rutaId)), primary: true },
    { key: 'paqueteId', header: 'Paquete', mono: true, render: (r, ctx) => ctx.label('paquetes', r.paqueteId), text: (r, ctx) => String(ctx.label('paquetes', r.paqueteId)) },
    { key: 'resultado', header: 'Resultado' },
    { key: 'fechaResultado', header: 'Fecha del resultado', mono: true, render: (r) => formatDateTime(r.fechaResultado), text: (r) => formatDateTime(r.fechaResultado) },
  ],
  fields: [
    { name: 'rutaId', label: 'Ruta', type: 'select', required: true, numeric: true, lookup: 'rutas', modes: ['create'], wide: true },
    {
      name: 'paqueteId', label: 'Paquete', type: 'select', required: true, numeric: true, modes: ['create'], wide: true,
      hint: 'Solo paquetes Registrado, En centro o Reprogramado del mismo centro de la ruta.',
      options: (values, _row, ctx) => {
        const ruta = ctx.lookups.rutas?.find((o) => String(o.value) === String(values.rutaId))?.raw;
        return (ctx.lookups.paquetes ?? []).filter((o) => ELEGIBLES.includes(o.raw.estado) && (!ruta || o.raw.centroDistId === ruta.centroDistId));
      },
    },
    { name: 'resultado', label: 'Resultado', type: 'select', required: true, modes: ['edit'], wide: true, options: [{ value: 'Entregado', label: 'Entregado' }, { value: 'Fallido', label: 'Fallido' }] },
  ],
};

const eventosConfig = {
  path: 'eventos', title: 'Eventos de seguimiento', singular: 'evento', newestFirst: true,
  intro: 'Historial que ve el cliente en el rastreo. Editarlo no cambia el estado del paquete.',
  getId: (r) => r.eventoId,
  describe: (r) => `el evento ${r.eventoId}`,
  lookups: ['paquetes'],
  api: {
    list: (s) => eventos.listEventos(s),
    create: (b) => eventos.createEvento(b),
    update: (row, b) => eventos.updateEvento(row.eventoId, { ...b, eventoId: row.eventoId }),
    remove: (row) => eventos.deleteEvento(row.eventoId),
  },
  columns: [
    { key: 'eventoId', header: 'Id', mono: true, numeric: true },
    { key: 'paqueteId', header: 'Paquete', mono: true, primary: true, render: (r, ctx) => ctx.label('paquetes', r.paqueteId), text: (r, ctx) => String(ctx.label('paquetes', r.paqueteId)) },
    { key: 'estado', header: 'Estado', render: (r) => <StatusBadge estado={r.estado} /> },
    { key: 'descripcion', header: 'Descripción' },
    { key: 'fecha', header: 'Fecha', mono: true, render: (r) => formatDateTime(r.fecha), text: (r) => formatDateTime(r.fecha) },
  ],
  fields: [
    { name: 'paqueteId', label: 'Paquete', type: 'select', required: true, numeric: true, lookup: 'paquetes', wide: true },
    { name: 'estado', label: 'Estado', type: 'select', required: true, options: ESTADO_OPTIONS },
    { name: 'fecha', label: 'Fecha y hora', type: 'datetime-local', required: true, defaultValue: nowLocalInput },
    { name: 'descripcion', label: 'Descripción', type: 'text', required: true, max: 255, wide: true },
  ],
};

const paquetesConfig = {
  path: 'paquetes', title: 'Paquetes', singular: 'paquete', newestFirst: true,
  intro: 'El código de seguimiento lo genera la base de datos. Una vez despachado, el servidor no permite cambiar carga, cliente, centro ni fecha compromiso.',
  getId: (r) => r.paqueteId,
  describe: (r) => `el paquete ${r.codigo ? shortCode(r.codigo) : r.paqueteId}`,
  deleteNote: 'Si el paquete tiene eventos o asignaciones asociados, el servidor rechazará la eliminación.',
  formNote: 'El estado solo avanza con las acciones de asignación y entrega; aquí puedes pasar un paquete Registrado a «En centro».',
  lookups: ['usuarios', 'clientes', 'centros'],
  api: {
    list: (s) => paquetes.listPaquetes({}, s),
    create: (b) => paquetes.createPaquete(b),
    update: (row, b) => paquetes.updatePaquete(row.paqueteId, b),
    remove: (row) => paquetes.deletePaquete(row.paqueteId),
  },
  buildPayload: (values, { mode, row }) => (mode === 'edit' ? { ...row, ...values } : values),
  columns: [
    { key: 'codigo', header: 'Código', mono: true, primary: true, render: (r) => <Link to={`/rastreo/${encodeURIComponent(r.codigo)}`}><TrackingCode code={r.codigo} /></Link> },
    { key: 'estado', header: 'Estado', render: (r) => <StatusBadge estado={r.estado} /> },
    { key: 'usuarioClienteId', header: 'Cliente', render: (r, ctx) => nameOfUser(ctx, r.usuarioClienteId), text: (r, ctx) => nameOfUser(ctx, r.usuarioClienteId) },
    { key: 'direccionDestino', header: 'Destino' },
    { key: 'peso', header: 'Peso', numeric: true, mono: true, render: (r) => `${formatKg(r.peso)}` },
    { key: 'volumen', header: 'Volumen', numeric: true, mono: true, render: (r) => `${formatM3(r.volumen)}` },
    { key: 'fechaCompromiso', header: 'Compromiso', mono: true, render: (r) => formatDate(r.fechaCompromiso), text: (r) => formatDate(r.fechaCompromiso) },
  ],
  fields: [
    { name: 'usuarioClienteId', label: 'Cliente', type: 'select', required: true, numeric: true, lookup: 'clientes', wide: true },
    { name: 'centroDistId', label: 'Centro de distribución', type: 'select', required: true, numeric: true, lookup: 'centros', wide: true },
    { name: 'peso', label: 'Peso (kg)', noun: 'el peso en kilos', type: 'decimal' },
    { name: 'volumen', label: 'Volumen (m³)', noun: 'el volumen en metros cúbicos', type: 'decimal' },
    { name: 'direccionDestino', label: 'Dirección de destino', type: 'text', required: true, max: 255, wide: true },
    { name: 'fechaCompromiso', label: 'Fecha compromiso', type: 'datetime-local', required: true, wide: true },
    {
      name: 'estado', label: 'Estado', type: 'select', modes: ['edit'], wide: true,
      options: (_values, row) => [
        { value: row.estado, label: row.estado },
        ...(row.estado === 'Registrado' ? [{ value: 'En centro', label: 'En centro' }] : []),
      ],
    },
  ],
};

export const RESOURCES = [
  paquetesConfig, rutasConfig, rutaPaqueteConfig, eventosConfig, usuariosConfig, centrosConfig, vehiculosConfig, repartidoresConfig,
];
