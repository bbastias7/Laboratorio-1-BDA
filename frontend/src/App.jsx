import { Route, Routes } from 'react-router-dom';
import { LoginRoute, RequireAuth, RequireRole } from './auth/guards.jsx';
import { AppShell } from './components/layout/AppShell.jsx';
import { LandingPage } from './pages/LandingPage.jsx';
import { TrackingPage } from './pages/TrackingPage.jsx';
import { NotFoundPage } from './pages/NotFoundPage.jsx';
import { ClienteRoutes } from './pages/cliente/ClienteRoutes.jsx';
import { RepartidorRoutes } from './pages/repartidor/RepartidorRoutes.jsx';
import { DespachadorRoutes } from './pages/despachador/DespachadorRoutes.jsx';

export function App() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<LandingPage />} />
        {/* Public entry: anonymous visitors see the code field and a login prompt (the API needs a token). */}
        <Route path="rastreo" element={<TrackingPage />} />
        <Route path="rastreo/:codigo" element={<TrackingPage />} />
        <Route path="login" element={<LoginRoute />} />

        <Route element={<RequireAuth />}>
          <Route element={<RequireRole roles={['cliente']} />}>
            <Route path="cliente/*" element={<ClienteRoutes />} />
          </Route>
          <Route element={<RequireRole roles={['repartidor']} />}>
            <Route path="repartidor/*" element={<RepartidorRoutes />} />
          </Route>
          <Route element={<RequireRole roles={['despachador']} />}>
            <Route path="despachador/*" element={<DespachadorRoutes />} />
          </Route>
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  );
}
