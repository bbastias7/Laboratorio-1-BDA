import { useEffect, useRef, useState } from 'react';
import { Link, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../../auth/AuthProvider.jsx';
import { ROLE_LABEL } from '../../auth/roles.js';
import { isNavActive, navFor } from '../../nav.js';
import { Icon } from '../Icon.jsx';
import { Button } from '../ui/Button.jsx';
import { Brand } from './Brand.jsx';
import { ThemeToggle } from './ThemeToggle.jsx';

/**
 * Page frame for every route: skip link, header with role-aware navigation (collapsible below 905 px, or 1240 px for roles with many links),
 * <main id="main"> and focus management on route change (focus moves to <main> so keyboard and
 * screen-reader users start at the new content). Screens render inside <Outlet />.
 */
export function AppShell() {
  const { status, user, login, logout } = useAuth();
  const location = useLocation();
  const mainRef = useRef(null);
  const first = useRef(true);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    setOpen(false);
    if (first.current) { first.current = false; return; }
    mainRef.current?.focus({ preventScroll: false });
    window.scrollTo(0, 0);
  }, [location.pathname]);

  useEffect(() => {
    if (!open) return undefined;
    const onKey = (e) => { if (e.key === 'Escape') setOpen(false); };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [open]);

  const authed = status === 'authenticated';
  const items = authed ? navFor(user.role) : [];
  // Roles with many links need more room before the full bar fits (see .nav-many in base.css).
  const many = items.length > 4;

  return (
    <div className="shell">
      <a className="skip-link" href="#main">Saltar al contenido</a>
      <header className="topbar">
        <div className="topbar-inner">
          <Brand />
          {authed && (
            <button
              type="button"
              className={`icon-btn nav-toggle ${many ? 'nav-toggle-many' : ''}`}
              aria-expanded={open}
              aria-controls="primary-nav"
              onClick={() => setOpen((o) => !o)}
              aria-label={open ? 'Cerrar menú' : 'Abrir menú'}
            >
              <Icon name={open ? 'close' : 'menu'} />
            </button>
          )}
          {authed && (
            <nav id="primary-nav" className={`nav ${many ? 'nav-many' : ''} ${open ? 'nav-open' : ''}`} aria-label="Principal">
              <ul>
                {items.map((it) => (
                  <li key={it.to}>
                    <Link to={it.to} className="nav-link" aria-current={isNavActive(it, location.pathname) ? 'page' : undefined}>
                      <Icon name={it.icon} size="sm" />
                      <span>{it.label}</span>
                    </Link>
                  </li>
                ))}
              </ul>
              <div className="nav-user">
                <span className="nav-user-name">
                  <Icon name="user" size="sm" />
                  <span>{user.name}<span className="nav-user-role"> · {ROLE_LABEL[user.role]}</span></span>
                </span>
                <Button variant="ghost" size="sm" icon="logout" onClick={logout}>Cerrar sesión</Button>
              </div>
            </nav>
          )}
          <div className="topbar-tools">
            {status === 'anonymous' && (
              <Button variant="primary" size="sm" icon="login" onClick={() => login('/')}>Ingresar</Button>
            )}
            <ThemeToggle />
          </div>
        </div>
      </header>
      <main id="main" ref={mainRef} tabIndex={-1} className="main">
        <Outlet />
      </main>
      <footer className="footer">
        <p>Logística G5 · Proyecto del Laboratorio 1 de Bases de Datos Avanzadas</p>
      </footer>
    </div>
  );
}
