import { useMemo, useRef } from 'react';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';
import { Icon } from '../Icon.jsx';
import { estadoMeta } from '../ui/StatusBadge.jsx';
import { formatDate, formatTime, parseLocal } from '../../lib/format.js';

gsap.registerPlugin(useGSAP);

const FINAL_STATES = ['Entregado', 'Devuelto a origen'];

// Text that accompanies the colour and icon of each state, so state never depends on colour alone.
const KIND_LABEL = {
  Registrado: 'Registrado',
  'En centro': 'En centro de distribución',
  'En reparto': 'En reparto',
  Entregado: 'Entregado',
  Reprogramado: 'Intento fallido · reprogramado',
  'Devuelto a origen': 'Devuelto a origen',
};

const byDate = (a, b) => (parseLocal(a.fecha)?.getTime() ?? 0) - (parseLocal(b.fecha)?.getTime() ?? 0);

/**
 * Vertical route line of a package: one node per event, oldest first (the order the backend returns),
 * the current state emphasised, failed attempts and returns visually distinct (shape + icon + text).
 * If the package has not reached a final state, a dashed "destination" node closes the line.
 *
 * Motion (GSAP via useGSAP, scoped to this component): each row fades/slides in with its line segment
 * drawing from the node above, staggered, transform + opacity only. With prefers-reduced-motion
 * (gsap.matchMedia) no tween runs and the final state is shown at once.
 *
 * Props: tracking (TrackingResponse). `animate` (default true) lets a parent turn the reveal off.
 */
export function ShipmentTimeline({ tracking, animate = true }) {
  const root = useRef(null);
  const events = useMemo(() => [...(tracking.eventos ?? [])].sort(byDate), [tracking.eventos]);
  const isFinal = FINAL_STATES.includes(tracking.estado);
  const lastIndex = events.length - 1;

  useGSAP(
    () => {
      if (!animate) return undefined;
      const mm = gsap.matchMedia(root.current);
      mm.add('(prefers-reduced-motion: no-preference)', () => {
        const rows = gsap.utils.toArray('.route-item');
        const segs = gsap.utils.toArray('.route-seg');
        const nodes = gsap.utils.toArray('.route-node');
        gsap.set(segs, { transformOrigin: 'top center' });
        // Total stagger is capped so a long history never takes more than ~1.2 s to reveal.
        const amount = Math.min(0.9, rows.length * 0.12);
        const tl = gsap.timeline({ defaults: { ease: 'power2.out' } });
        tl.from(rows, { opacity: 0, y: 14, duration: 0.32, stagger: { amount } }, 0)
          .from(nodes, { scale: 0.4, duration: 0.28, stagger: { amount } }, 0)
          .from(segs, { scaleY: 0, duration: 0.3, stagger: { amount }, ease: 'none' }, 0.1);
        const current = root.current?.querySelector('.route-item-current .route-node');
        if (current) {
          tl.fromTo(current, { boxShadow: '0 0 0 0px var(--color-selection)' }, { boxShadow: '0 0 0 6px var(--color-selection)', duration: 0.5, yoyo: true, repeat: 1 });
        }
        return () => tl.kill();
      });
      return () => mm.revert();
    },
    { scope: root, dependencies: [tracking.codigo, events.length, animate], revertOnUpdate: true },
  );

  return (
    <section className="route-wrap" aria-labelledby="route-title">
      <h2 id="route-title" className="route-title">Recorrido del envío</h2>
      <ol className="route" ref={root}>
        {events.map((ev, i) => {
          const meta = estadoMeta(ev.estado);
          const current = i === lastIndex;
          const failed = ev.estado === 'Reprogramado';
          const returned = ev.estado === 'Devuelto a origen';
          const hasNext = i < lastIndex || !isFinal;
          return (
            <li
              key={`${ev.fecha}-${i}`}
              className={`route-item tone-${meta.tone} ${current ? 'route-item-current' : ''} ${failed ? 'route-item-failed' : ''} ${returned ? 'route-item-returned' : ''}`}
              aria-current={current ? 'step' : undefined}
            >
              <time className="route-time" dateTime={ev.fecha}>
                <span>{formatDate(ev.fecha)}</span>
                <span className="route-hour">{formatTime(ev.fecha)}</span>
              </time>
              <span className={`route-node node-${meta.tone} ${ev.estado === 'En centro' ? 'node-square' : ''}`}>
                <Icon name={meta.icon} size="sm" />
              </span>
              {hasNext && <span className={`route-seg ${i === lastIndex ? 'route-seg-pending' : ''}`} aria-hidden="true" />}
              <div className="route-body">
                <p className="route-state">
                  {KIND_LABEL[ev.estado] ?? ev.estado}
                  {current && <span className="route-now">Estado actual</span>}
                </p>
                {ev.descripcion && <p className="route-desc">{ev.descripcion}</p>}
              </div>
            </li>
          );
        })}
        {!isFinal && (
          <li className="route-item route-item-dest">
            <span className="route-time"><span className="route-hour">Pendiente</span></span>
            <span className="route-node node-pending"><Icon name="pin" size="sm" /></span>
            <div className="route-body">
              <p className="route-state">Entrega en destino</p>
              <p className="route-desc">{tracking.direccionDestino}</p>
            </div>
          </li>
        )}
      </ol>
    </section>
  );
}
