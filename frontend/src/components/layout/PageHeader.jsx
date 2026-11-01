/** Screen heading block. Props: title (h1), eyebrow (small label above), actions (node, right aligned), children (intro text). */
export function PageHeader({ title, eyebrow, actions, children }) {
  return (
    <header className="page-header">
      <div className="page-header-text">
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        <h1>{title}</h1>
        {children && <p className="page-intro">{children}</p>}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </header>
  );
}
