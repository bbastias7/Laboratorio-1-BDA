import { Navigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthProvider.jsx';
import { homeFor } from '../auth/roles.js';
import { useDocumentTitle } from '../hooks/useDocumentTitle.js';
import { Loading } from '../components/ui/States.jsx';
import { AccountProblem } from '../auth/guards.jsx';
import { ShipmentSearch } from '../components/shipment/ShipmentSearch.jsx';
import { ShipmentTimeline } from '../components/shipment/ShipmentTimeline.jsx';

// Illustrative data, labelled as an example on screen. Never presented as a real package.
const SAMPLE = {
  codigo: 'EJEMPLO-001',
  estado: 'En reparto',
  direccionDestino: 'Dirección de ejemplo 123',
  intentos: 1,
  eventos: [
    { estado: 'Registrado', descripcion: 'Paquete registrado', fecha: '2026-03-02T09:10:00' },
    { estado: 'En centro', descripcion: 'Recibido en el centro de distribución', fecha: '2026-03-02T15:40:00' },
    { estado: 'Reprogramado', descripcion: 'Intento fallido 1 (nadie en casa); reprogramado', fecha: '2026-03-03T11:25:00' },
    { estado: 'En reparto', descripcion: 'Asignado a ruta', fecha: '2026-03-04T08:05:00' },
  ],
};

/** Public entry. Signed-in users go straight to their role home. */
export function LandingPage() {
  const { status, user, login } = useAuth();
  useDocumentTitle('');

  if (status === 'loading') return <Loading label="Cargando" />;
  if (status === 'error') return <AccountProblem />;
  if (status === 'authenticated') return <Navigate to={homeFor(user.role)} replace />;

  return (
    <div className="landing">
      <section className="landing-main" aria-labelledby="landing-title">
        <p className="eyebrow">Seguimiento de envíos</p>
        <h1 id="landing-title" className="display">¿Dónde está tu envío?</h1>
        <p className="lead">Escribe el código y ve cada paso del recorrido, incluidos los intentos de entrega que no resultaron.</p>
        <ShipmentSearch
          submitLabel="Ingresar para rastrear"
          onSubmit={(code) => login(`/rastreo/${encodeURIComponent(code)}`)}
        />
        <p className="notice">Rastrear requiere iniciar sesión. Después de ingresar abrimos tu código automáticamente.</p>
      </section>

      <aside className="landing-sample" aria-label="Ejemplo ilustrativo de un recorrido">
        <p className="eyebrow">Ejemplo ilustrativo</p>
        <div className="sample" data-nsd-interaction="tracking-code">
          <ShipmentTimeline tracking={SAMPLE} />
        </div>
      </aside>
    </div>
  );
}
