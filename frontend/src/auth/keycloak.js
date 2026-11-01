import Keycloak from 'keycloak-js';
import { config } from '../config.js';

// One Keycloak instance for the whole app (never create it inside a component).
export const keycloak = new Keycloak({
  url: config.keycloak.url,
  realm: config.keycloak.realm,
  clientId: config.keycloak.clientId,
});

let initPromise = null;

/**
 * Initialises keycloak-js once (React StrictMode runs effects twice in dev).
 * `check-sso` keeps the landing/tracking entry public and only restores an existing session,
 * through public/silent-check-sso.html in a hidden iframe. Authorization code flow + PKCE S256.
 * Resolves to `true` when a session exists.
 */
export function initKeycloak() {
  if (!initPromise) {
    initPromise = keycloak.init({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false,
      silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
    });
  }
  return initPromise;
}

/** Starts login. `path` is where the user lands afterwards (absolute path inside the app, e.g. "/rastreo/ABC"). */
export function login(path = '/') {
  return keycloak.login({ redirectUri: `${window.location.origin}${path}`, locale: 'es' });
}

export function logout() {
  return keycloak.logout({ redirectUri: `${window.location.origin}/` });
}

/**
 * Returns a token valid for at least 30 more seconds, refreshing it when needed.
 * If the refresh fails the session is gone: restart login and reject.
 */
export async function getFreshToken({ force = false } = {}) {
  if (!keycloak.authenticated) throw new Error('No hay sesión activa');
  try {
    await keycloak.updateToken(force ? -1 : 30);
  } catch (err) {
    login(window.location.pathname + window.location.search);
    throw err;
  }
  return keycloak.token;
}
