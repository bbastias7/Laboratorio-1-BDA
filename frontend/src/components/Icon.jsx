// Inline SVG icon set: 24 px grid, 1.75 px stroke, currentColor. Decorative by default (aria-hidden);
// pass `label` when the icon is the only content of a control or conveys meaning on its own.
const PATHS = {
  search: <><circle cx="11" cy="11" r="6.5" /><path d="M16 16l4.5 4.5" /></>,
  copy: <><rect x="8.5" y="8.5" width="11" height="11" rx="2" /><path d="M5 15.5V6.5A1.5 1.5 0 0 1 6.5 5h9" /></>,
  check: <path d="M5 12.5l4.5 4.5L19 7.5" />,
  alert: <><path d="M12 4.5l8.5 15h-17z" /><path d="M12 10.5v4" /><path d="M12 17.2v.1" /></>,
  return: <><path d="M9 7L4.5 11.5 9 16" /><path d="M5 11.5h9.5a4.5 4.5 0 0 1 0 9H12" /></>,
  box: <><path d="M12 3.5l8 4v9l-8 4-8-4v-9z" /><path d="M4 7.5l8 4 8-4" /><path d="M12 11.5v9" /></>,
  truck: <><path d="M3 6.5h11v9H3z" /><path d="M14 9.5h3.8l3.2 3.2v2.8h-7" /><circle cx="7.5" cy="17.5" r="1.8" /><circle cx="17" cy="17.5" r="1.8" /></>,
  pin: <><path d="M12 21s6.5-5.6 6.5-11a6.5 6.5 0 0 0-13 0c0 5.4 6.5 11 6.5 11z" /><circle cx="12" cy="10" r="2.3" /></>,
  building: <><path d="M5 20.5V5.5h9v15" /><path d="M14 10.5h5v10" /><path d="M8.5 9h2M8.5 12.5h2M8.5 16h2" /><path d="M3.5 20.5h17" /></>,
  clock: <><circle cx="12" cy="12" r="8.5" /><path d="M12 7.5V12l3 2" /></>,
  lock: <><rect x="5" y="10.5" width="14" height="9.5" rx="2" /><path d="M8 10.5V8a4 4 0 0 1 8 0v2.5" /></>,
  logout: <><path d="M10 4.5H6.5A1.5 1.5 0 0 0 5 6v12a1.5 1.5 0 0 0 1.5 1.5H10" /><path d="M14 8l4 4-4 4" /><path d="M18 12H9.5" /></>,
  login: <><path d="M14 4.5h3.5A1.5 1.5 0 0 1 19 6v12a1.5 1.5 0 0 1-1.5 1.5H14" /><path d="M10 8l4 4-4 4" /><path d="M14 12H5" /></>,
  menu: <path d="M4 7h16M4 12h16M4 17h16" />,
  close: <path d="M6 6l12 12M18 6L6 18" />,
  sun: <><circle cx="12" cy="12" r="4" /><path d="M12 3v2M12 19v2M3 12h2M19 12h2M5.6 5.6l1.4 1.4M17 17l1.4 1.4M5.6 18.4L7 17M17 7l1.4-1.4" /></>,
  moon: <path d="M19.5 14.5A8 8 0 0 1 9.5 4.5a8 8 0 1 0 10 10z" />,
  circle: <circle cx="12" cy="12" r="6.5" />,
  square: <rect x="6" y="6" width="12" height="12" rx="1.5" />,
  refresh: <><path d="M19 8a7.5 7.5 0 0 0-13-1.5L4.5 8" /><path d="M4.5 4v4h4" /><path d="M5 16a7.5 7.5 0 0 0 13 1.5l1.5-1.5" /><path d="M19.5 20v-4h-4" /></>,
  plus: <path d="M12 5v14M5 12h14" />,
  info: <><circle cx="12" cy="12" r="8.5" /><path d="M12 11v5" /><path d="M12 8v.1" /></>,
  user: <><circle cx="12" cy="8.5" r="3.5" /><path d="M5 20c.8-3.6 3.6-5.5 7-5.5s6.2 1.9 7 5.5" /></>,
  list: <><path d="M9 7h11M9 12h11M9 17h11" /><path d="M4.5 7h.1M4.5 12h.1M4.5 17h.1" /></>,
  chevron: <path d="M9 5l7 7-7 7" />,
  edit: <><path d="M5 19l1-4 9.5-9.5a2 2 0 0 1 2.8 0l.2.2a2 2 0 0 1 0 2.8L9 18z" /><path d="M14 7l3 3" /></>,
  trash: <><path d="M4.5 7h15" /><path d="M9.5 7V4.5h5V7" /><path d="M6.5 7l1 12.5h9L17.5 7" /><path d="M10 11v5M14 11v5" /></>,
  chart: <><path d="M4.5 4.5v15h15" /><path d="M8.5 16v-4M12.5 16V8M16.5 16v-6" /></>,
  route: <><circle cx="6.5" cy="6.5" r="2" /><circle cx="17.5" cy="17.5" r="2" /><path d="M8.5 6.5H14a3 3 0 0 1 0 6h-4a3 3 0 0 0 0 6h5.5" /></>,
  wifi: <><path d="M3.5 9.5a12 12 0 0 1 17 0" /><path d="M6.5 12.5a8 8 0 0 1 11 0" /><path d="M9.5 15.5a4 4 0 0 1 5 0" /><path d="M12 19v.1" /></>,
};

export function Icon({ name, size = 'md', label, className = '', ...rest }) {
  const body = PATHS[name];
  if (!body) return null;
  const a11y = label ? { role: 'img', 'aria-label': label } : { 'aria-hidden': true, focusable: 'false' };
  return (
    <svg
      className={`icon icon-${size} ${className}`.trim()}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.75"
      strokeLinecap="round"
      strokeLinejoin="round"
      {...a11y}
      {...rest}
    >
      {body}
    </svg>
  );
}
