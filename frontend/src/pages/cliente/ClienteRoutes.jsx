import { Navigate, Route, Routes } from 'react-router-dom';
import { ClienteHome } from './ClienteHome.jsx';
import { ClienteNuevo } from './ClienteNuevo.jsx';
import { ClienteEnvio } from './ClienteEnvio.jsx';

// Routes under /cliente/*. Links live in src/nav.js.
export function ClienteRoutes() {
  return (
    <Routes>
      <Route index element={<ClienteHome />} />
      <Route path="nuevo" element={<ClienteNuevo />} />
      <Route path="envios/:id" element={<ClienteEnvio />} />
      <Route path="*" element={<Navigate to="/cliente" replace />} />
    </Routes>
  );
}
