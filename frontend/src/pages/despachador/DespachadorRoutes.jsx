import { Navigate, Route, Routes } from 'react-router-dom';
import { DespachadorHome } from './DespachadorHome.jsx';
import { AsignarRuta } from './AsignarRuta.jsx';
import { Mantenedores } from './Mantenedores.jsx';
import { Auditoria } from './Auditoria.jsx';
import { Reportes } from './Reportes.jsx';

// Routes under /despachador/*. Links live in src/nav.js.
export function DespachadorRoutes() {
  return (
    <Routes>
      <Route index element={<DespachadorHome />} />
      <Route path="rutas/asignar" element={<AsignarRuta />} />
      <Route path="mantenedores/*" element={<Mantenedores />} />
      <Route path="auditoria" element={<Auditoria />} />
      <Route path="reportes" element={<Reportes />} />
      <Route path="*" element={<Navigate to="/despachador" replace />} />
    </Routes>
  );
}
