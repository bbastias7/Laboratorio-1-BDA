// Typed API failure. `kind` is derived from the HTTP status so screens can branch without magic numbers.
const KIND_BY_STATUS = {
  400: 'validation',
  401: 'unauthorized',
  403: 'forbidden',
  404: 'notFound',
  405: 'validation',
  409: 'conflict',
};

export class ApiError extends Error {
  /**
   * @param {number} status HTTP status, or 0 when the server could not be reached
   * @param {string} message message from the backend `{ "error": "..." }` body, already in Spanish
   */
  constructor(status, message) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.kind = status === 0 ? 'network' : (KIND_BY_STATUS[status] ?? (status >= 500 ? 'server' : 'unknown'));
  }
  get isForbidden() { return this.kind === 'forbidden'; }
  get isNotFound() { return this.kind === 'notFound'; }
  get isConflict() { return this.kind === 'conflict'; }
  get isNetwork() { return this.kind === 'network'; }
}

export function errorMessage(err, fallback = 'Ocurrió un error inesperado. Inténtalo de nuevo.') {
  return err instanceof ApiError || err instanceof Error ? err.message || fallback : fallback;
}
