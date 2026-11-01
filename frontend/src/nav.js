// Role navigation. F4-F6 add their screens here (one entry per route) and in the role's *Routes.jsx file.
// `to` is absolute; `end` makes the item active only on an exact match of `to`; `also` lists extra path
// prefixes that belong to the same section (e.g. the shipment detail belongs to "Mis envíos").
const under = (pathname, base) => pathname === base || pathname.startsWith(`${base}/`);

/** True when the current pathname belongs to the nav item, including nested routes. */
export const isNavActive = (item, pathname) =>
  (item.end ? pathname === item.to : under(pathname, item.to)) || (item.also ?? []).some((base) => under(pathname, base));

export const COMMON_NAV = [{ to: '/rastreo', label: 'Rastrear', icon: 'search' }];

export const ROLE_NAV = {
  cliente: [
    { to: '/cliente', label: 'Mis envíos', icon: 'box', end: true, also: ['/cliente/envios'] },
    { to: '/cliente/nuevo', label: 'Crear envío', icon: 'plus' },
  ],
  repartidor: [
    { to: '/repartidor', label: 'Mis entregas', icon: 'truck', end: true },
    { to: '/repartidor/rutas', label: 'Mis rutas', icon: 'route' },
  ],
  despachador: [
    { to: '/despachador', label: 'Operación', icon: 'list', end: true },
    { to: '/despachador/rutas/asignar', label: 'Asignar ruta', icon: 'route' },
    { to: '/despachador/mantenedores', label: 'Mantenedores', icon: 'building' },
    { to: '/despachador/auditoria', label: 'Auditoría', icon: 'clock' },
    { to: '/despachador/reportes', label: 'Reportes', icon: 'chart' },
  ],
};

export const navFor = (role) => [...(ROLE_NAV[role] ?? []), ...COMMON_NAV];
