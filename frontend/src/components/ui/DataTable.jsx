import { useEffect, useRef, useState } from 'react';

/**
 * Responsive table. From 600 px up it is a normal table; below, every row becomes a stacked block and each cell
 * prints its column header (data-label). ARIA roles are set explicitly so the stacked layout keeps table semantics.
 * A table wider than its container scrolls inside the wrapper (never the page); only then the wrapper becomes a
 * focusable named region so keyboard users can scroll it.
 *
 * Props:
 *  - caption: accessible name of the table (visually hidden)
 *  - columns: [{ key, header, render?(row), mono?, numeric?, primary?, hideHeader? }]
 *  - rows, rowKey(row)
 *  - rowActions?(row): node rendered in a last "Acciones" column
 *  - dense: smaller row height (desktop operations tables)
 */
export function DataTable({ caption, columns, rows, rowKey, rowActions, dense = false, className = '' }) {
  const wrapRef = useRef(null);
  const [scrollable, setScrollable] = useState(false);

  useEffect(() => {
    const wrap = wrapRef.current;
    if (!wrap) return undefined;
    const measure = () => setScrollable(wrap.scrollWidth > wrap.clientWidth + 1);
    measure();
    const observer = new ResizeObserver(measure);
    observer.observe(wrap);
    if (wrap.firstElementChild) observer.observe(wrap.firstElementChild);
    return () => observer.disconnect();
  }, [rows, columns]);

  return (
    <div
      ref={wrapRef}
      className={`table-wrap ${className}`.trim()}
      {...(scrollable ? { role: 'region', 'aria-label': `${caption} (desplazable horizontalmente)`, tabIndex: 0 } : {})}
    >
      <table className={`table ${dense ? 'table-dense' : ''}`} role="table">
        <caption className="sr-only">{caption}</caption>
        <thead role="rowgroup">
          <tr role="row">
            {columns.map((c) => (
              <th key={c.key} scope="col" role="columnheader" className={c.numeric ? 'num' : undefined}>
                {c.hideHeader ? <span className="sr-only">{c.header}</span> : c.header}
              </th>
            ))}
            {rowActions && <th scope="col" role="columnheader"><span className="sr-only">Acciones</span></th>}
          </tr>
        </thead>
        <tbody role="rowgroup">
          {rows.map((row) => (
            <tr key={rowKey(row)} role="row">
              {columns.map((c) => (
                <td
                  key={c.key}
                  role="cell"
                  data-label={c.hideHeader ? undefined : c.header}
                  data-primary={c.primary ? '' : undefined}
                  className={[c.mono && 'mono', c.numeric && 'num'].filter(Boolean).join(' ') || undefined}
                >
                  {c.render ? c.render(row) : (row[c.key] ?? '—')}
                </td>
              ))}
              {rowActions && <td role="cell" className="cell-actions">{rowActions(row)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
