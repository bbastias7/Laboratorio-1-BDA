import { Icon } from '../Icon.jsx';

/** Visual language of a package state: word + icon + status tone. Never colour alone. */
export const ESTADOS = {
  Registrado: { tone: 'neutral', icon: 'circle', word: 'Registrado' },
  'En centro': { tone: 'info', icon: 'building', word: 'En centro' },
  'En reparto': { tone: 'brand', icon: 'truck', word: 'En reparto' },
  Entregado: { tone: 'success', icon: 'check', word: 'Entregado' },
  Reprogramado: { tone: 'warning', icon: 'alert', word: 'Reprogramado' },
  'Devuelto a origen': { tone: 'danger', icon: 'return', word: 'Devuelto a origen' },
};

export const estadoMeta = (estado) => ESTADOS[estado] ?? { tone: 'neutral', icon: 'info', word: estado ?? 'Sin estado' };

/** Badge for a package estado. Props: estado (one of the six backend values), size "md" | "lg". */
export function StatusBadge({ estado, size = 'md' }) {
  const meta = estadoMeta(estado);
  return (
    <span className={`badge badge-${meta.tone} badge-${size}`}>
      <Icon name={meta.icon} size="sm" />
      <span>{meta.word}</span>
    </span>
  );
}
