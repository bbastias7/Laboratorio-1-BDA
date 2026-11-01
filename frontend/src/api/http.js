import { config } from '../config.js';
import { getFreshToken } from '../auth/keycloak.js';
import { ApiError } from './ApiError.js';

function buildUrl(path, query) {
  const url = new URL(config.apiUrl + path);
  if (query) {
    Object.entries(query).forEach(([k, v]) => {
      if (v !== undefined && v !== null && v !== '') url.searchParams.set(k, String(v));
    });
  }
  return url;
}

async function send(url, { method, body, signal }, token) {
  const headers = { Accept: 'application/json', Authorization: `Bearer ${token}` };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  try {
    return await fetch(url, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (err) {
    if (err?.name === 'AbortError') throw err;
    throw new ApiError(0, 'No pudimos conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.');
  }
}

async function toApiError(res) {
  let message = '';
  try {
    const data = await res.json(); // backend format: { "error": "texto" }
    message = typeof data?.error === 'string' ? data.error : '';
  } catch { /* body is not JSON */ }
  return new ApiError(res.status, message || `La solicitud falló (código ${res.status}).`);
}

/**
 * Authenticated JSON request against the backend.
 * - Adds `Authorization: Bearer <token>` after refreshing it (updateToken(30)).
 * - 401: refreshes the token once and retries. If it is still 401 the account has no usable
 *   profile/role in the API, so an ApiError(401) is thrown (the shell shows an account-state screen
 *   instead of looping through Keycloak).
 * - 403: ApiError(403) (kind "forbidden"); callers render a "no autorizado" state.
 * - 204: resolves to null.
 * @returns {Promise<any>} parsed JSON body
 */
export async function request(path, { method = 'GET', body, query, signal } = {}) {
  const url = buildUrl(path, query);
  let res = await send(url, { method, body, signal }, await getFreshToken());
  if (res.status === 401) {
    res = await send(url, { method, body, signal }, await getFreshToken({ force: true }));
  }
  if (!res.ok) throw await toApiError(res);
  if (res.status === 204) return null;
  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

export const get = (path, query, signal) => request(path, { query, signal });
export const post = (path, body, signal) => request(path, { method: 'POST', body, signal });
export const put = (path, body, signal) => request(path, { method: 'PUT', body, signal });
export const patch = (path, body, signal) => request(path, { method: 'PATCH', body, signal });
export const del = (path, signal) => request(path, { method: 'DELETE', signal });
