import { NavLink, Navigate, Route, Routes } from 'react-router-dom';
import { PageHeader } from '../../components/layout/PageHeader.jsx';
import { ResourceCrud } from '../../components/crud/ResourceCrud.jsx';
import { RESOURCES } from './resources.jsx';

/** /despachador/mantenedores/:recurso. One generic screen per resource, switched with the sub-navigation. */
export function Mantenedores() {
  return (
    <div className="page">
      <PageHeader title="Mantenedores" eyebrow="Despachador">
        Crea, edita y elimina los datos maestros y operativos. Las reglas del negocio las valida el servidor.
      </PageHeader>
      <nav className="subnav" aria-label="Mantenedores">
        <ul>
          {RESOURCES.map((r) => (
            <li key={r.path}><NavLink to={`/despachador/mantenedores/${r.path}`}>{r.title}</NavLink></li>
          ))}
        </ul>
      </nav>
      <Routes>
        {RESOURCES.map((r) => <Route key={r.path} path={r.path} element={<ResourceCrud key={r.path} config={r} />} />)}
        <Route path="*" element={<Navigate to={`/despachador/mantenedores/${RESOURCES[0].path}`} replace />} />
      </Routes>
    </div>
  );
}
