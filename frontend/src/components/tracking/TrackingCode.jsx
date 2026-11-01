import { groupCode, shortCode } from '../../lib/code.js';

/**
 * Readable display of a tracking code. The raw string is always what a screen reader announces,
 * what is copied (selection copy is overridden) and what the API receives.
 *  - variant "short" (tables, cards, dialogs): "G5-3f9a…b2c1", full code in the tooltip.
 *  - variant "full" (headers, detail views): eight blocks of four characters, wrapping at block borders.
 * Props: code, variant, as (element), id, className.
 */
export function TrackingCode({ code, variant = 'short', as: Tag = 'span', className = '', ...rest }) {
  const grouped = variant === 'full' ? groupCode(code) : null;
  const onCopy = (e) => {
    // Only when the selection lies inside this code: copy the exact original string, not the visual form.
    const sel = window.getSelection();
    if (!sel || sel.isCollapsed || !e.currentTarget.contains(sel.anchorNode) || !e.currentTarget.contains(sel.focusNode)) return;
    e.clipboardData.setData('text/plain', code);
    e.preventDefault();
  };
  const cls = `code ${grouped ? 'code-full' : 'code-short'} ${className}`.trim();

  if (grouped) {
    return (
      <Tag className={cls} onCopy={onCopy} {...rest}>
        <span className="sr-only">{code}</span>
        <span aria-hidden="true" className="code-blocks">
          <span className="code-block">{grouped.prefix}{grouped.groups.slice(0, 1)}</span>
          {grouped.groups.slice(1).map((g, i) => <span className="code-block" key={i}>{g}</span>)}
        </span>
      </Tag>
    );
  }
  return (
    <Tag className={cls} title={code} onCopy={onCopy} {...rest}>
      <span className="sr-only">{code}</span>
      <span aria-hidden="true">{shortCode(code)}</span>
    </Tag>
  );
}
