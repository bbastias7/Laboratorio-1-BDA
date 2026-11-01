import { Navigate, Route, Routes } from 'react-router-dom';
import { RepartidorHome } from './RepartidorHome.jsx';
import { RepartidorRutaDetalle, RepartidorRutas } from './RepartidorRutas.jsx';

// Routes under /repartidor/*. Links live in src/nav.js.
export function RepartidorRoutes() {
  return (
    <Routes>
      <Route index element={<RepartidorHome />} />
      <Route path="rutas" element={<RepartidorRutas />} />
      <Route path="rutas/:id" element={<RepartidorRutaDetalle />} />
      <Route path="*" element={<Navigate to="/repartidor" replace />} />
    </Routes>
  );
}
