import { useEffect, useRef, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { auth } from '../api';
import { useAuth } from '../auth/AuthProvider.jsx';
import { homeFor } from '../auth/roles.js';
import { useDocumentTitle } from '../hooks/useDocumentTitle.js';
import { PageHeader } from '../components/layout/PageHeader.jsx';
import { Alert } from '../components/ui/Alert.jsx';
import { Button, LinkButton } from '../components/ui/Button.jsx';
import { Field, TextInput } from '../components/ui/Field.jsx';
import { Loading } from '../components/ui/States.jsx';
import { explainError } from '../lib/serverMessages.js';

const EMPTY = { nombre: '', email: '', password: '', confirmacion: '' };
const FIELD_ORDER = ['nombre', 'email', 'password', 'confirmacion'];

// Mirrors the backend rules (RegistroRequest): nombre ≤ 64, correo válido ≤ 128, contraseña de 8 a 128 caracteres.
function validate(v) {
  const e = {};
  const nombre = v.nombre.trim();
  const email = v.email.trim();
  if (!nombre) e.nombre = 'Ingresa tu nombre.';
  else if (nombre.length > 64) e.nombre = 'El nombre admite hasta 64 caracteres.';
  if (!email) e.email = 'Ingresa tu correo electrónico.';
  else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) e.email = 'Ingresa un correo válido, por ejemplo nombre@dominio.cl.';
  else if (email.length > 128) e.email = 'El correo admite hasta 128 caracteres.';
  if (!v.password) e.password = 'Crea una contraseña.';
  else if (v.password.length < 8) e.password = 'La contraseña debe tener al menos 8 caracteres.';
  else if (v.password.length > 128) e.password = 'La contraseña admite hasta 128 caracteres.';
  if (!v.confirmacion) e.confirmacion = 'Repite la contraseña.';
  else if (v.confirmacion !== v.password) e.confirmacion = 'Las contraseñas no coinciden.';
  return e;
}

/** Public sign-up for clients: POST /api/auth/registro (no token). The role is always cliente, decided by the server. */
export function RegistroPage() {
  useDocumentTitle('Crear cuenta');
  const { status, user, login } = useAuth();
  const [values, setValues] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState(null);
  const [busy, setBusy] = useState(false);
  const [created, setCreated] = useState(null);
  const formRef = useRef(null);
  const errorRef = useRef(null);
  const doneRef = useRef(null);

  useEffect(() => { if (serverError) errorRef.current?.focus(); }, [serverError]);
  useEffect(() => { if (created) doneRef.current?.focus(); }, [created]);

  if (status === 'loading') return <Loading label="Cargando" />;
  if (status === 'authenticated') return <Navigate to={homeFor(user.role)} replace />;

  const set = (name) => (e) => {
    setValues((v) => ({ ...v, [name]: e.target.value }));
    setErrors((er) => ({ ...er, [name]: undefined }));
  };

  const focusFirstInvalid = () => requestAnimationFrame(() => formRef.current?.querySelector('[aria-invalid="true"]')?.focus());

  const submit = async (e) => {
    e.preventDefault();
    setServerError(null);
    const found = validate(values);
    const ordered = Object.fromEntries(FIELD_ORDER.filter((k) => found[k]).map((k) => [k, found[k]]));
    setErrors(ordered);
    if (Object.keys(ordered).length) { focusFirstInvalid(); return; }
    setBusy(true);
    try {
      const profile = await auth.registrar({ nombre: values.nombre.trim(), email: values.email.trim(), password: values.password });
      setCreated(profile);
    } catch (err) {
      const explained = explainError(err);
      const field = /^(nombre|email|password): (.+)$/.exec(explained.message);
      if (err.status === 409) {
        setErrors({ email: 'Ya existe una cuenta con este correo. Ingresa con ella o usa otro correo.' });
        focusFirstInvalid();
      } else if (field) {
        setErrors({ [field[1]]: field[2] });
        focusFirstInvalid();
      } else {
        setServerError(explained);
      }
    } finally {
      setBusy(false);
    }
  };

  if (created) {
    return (
      <div className="page">
        <PageHeader title="Cuenta creada" eyebrow="Crear cuenta" />
        <section className="created" aria-labelledby="registro-ok">
          <div ref={doneRef} tabIndex={-1} className="focus-target" role="status">
            <Alert tone="success" title="¡Listo! Tu cuenta quedó creada.">
              <span id="registro-ok">Ya puedes ingresar con <strong>{created.email}</strong> y la contraseña que elegiste.</span>
            </Alert>
          </div>
          <div className="form-actions">
            <Button variant="primary" icon="login" onClick={() => login('/')}>Ingresar</Button>
            <LinkButton to="/">Volver al inicio</LinkButton>
          </div>
        </section>
      </div>
    );
  }

  return (
    <div className="page">
      <PageHeader title="Crear cuenta" eyebrow="Nuevo cliente">
        Regístrate para crear envíos y seguir cada paso de su recorrido.
      </PageHeader>
      <form ref={formRef} className="form form-narrow" onSubmit={submit} noValidate>
        {serverError && (
          <Alert ref={errorRef} tone="danger" title="No se pudo crear la cuenta" detail={serverError.detail}>
            {serverError.message}
          </Alert>
        )}
        <div className="form-grid">
          <Field label="Nombre" required error={errors.nombre} className="field-wide">
            <TextInput autoComplete="name" maxLength={64} value={values.nombre} onChange={set('nombre')} />
          </Field>
          <Field label="Correo electrónico" required error={errors.email} className="field-wide">
            <TextInput type="email" autoComplete="email" maxLength={128} value={values.email} onChange={set('email')} />
          </Field>
          <Field label="Contraseña" required error={errors.password} hint="Mínimo 8 caracteres.">
            <TextInput type="password" autoComplete="new-password" maxLength={128} value={values.password} onChange={set('password')} />
          </Field>
          <Field label="Confirmar contraseña" required error={errors.confirmacion}>
            <TextInput type="password" autoComplete="new-password" maxLength={128} value={values.confirmacion} onChange={set('confirmacion')} />
          </Field>
        </div>
        <div className="form-actions">
          <Button type="submit" variant="primary" loading={busy}>Crear cuenta</Button>
          <Button variant="ghost" onClick={() => login('/')}>Ya tengo cuenta</Button>
        </div>
      </form>
    </div>
  );
}
