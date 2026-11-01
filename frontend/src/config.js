// Runtime configuration from Vite env vars. Defaults match the local docker/dev setup (see .env.example).
const env = import.meta.env;
const trim = (v) => String(v).replace(/\/+$/, '');

export const config = {
  apiUrl: trim(env.VITE_API_URL || 'http://localhost:8080'),
  keycloak: {
    url: trim(env.VITE_KEYCLOAK_URL || 'http://localhost:8081'),
    realm: env.VITE_KEYCLOAK_REALM || 'logistica-g5',
    clientId: env.VITE_KEYCLOAK_CLIENT || 'logistica-web',
  },
};
