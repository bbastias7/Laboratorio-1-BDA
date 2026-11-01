import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Port 5173 is fixed: the Keycloak client `logistica-web` only allows
// http://localhost:5173/* as redirect URI and web origin.
export default defineConfig({
  plugins: [react()],
  server: { port: 5173, strictPort: true },
  preview: { port: 5173, strictPort: true },
});
