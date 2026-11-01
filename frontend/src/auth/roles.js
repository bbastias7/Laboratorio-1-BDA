// Role model shared by routing, navigation and guards.
export const ROLES = ['cliente', 'repartidor', 'despachador'];

export const ROLE_LABEL = {
  cliente: 'Cliente',
  repartidor: 'Repartidor',
  despachador: 'Despachador',
};

// Landing route of each role after login.
export const ROLE_HOME = {
  cliente: '/cliente',
  repartidor: '/repartidor',
  despachador: '/despachador',
};

// Priority only matters if a token ever carries several roles; the backend rejects that case anyway.
const PRIORITY = ['despachador', 'repartidor', 'cliente'];

/** Realm roles of a parsed token intersected with the application roles. */
export function rolesFromToken(tokenParsed) {
  const roles = tokenParsed?.realm_access?.roles;
  return Array.isArray(roles) ? ROLES.filter((r) => roles.includes(r)) : [];
}

export function pickRole(roles) {
  return PRIORITY.find((r) => roles.includes(r)) ?? null;
}

export const homeFor = (role) => ROLE_HOME[role] ?? '/';
