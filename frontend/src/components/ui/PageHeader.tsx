import type { ReactNode } from 'react';

interface PageHeaderProps {
  title: ReactNode;
  eyebrow?: string;
  description?: ReactNode;
  actions?: ReactNode;
  /** Small element next to the title, such as a count chip. */
  badge?: ReactNode;
  illustration?: string;
}

export function PageHeader({ title, eyebrow, description, actions, badge, illustration }: PageHeaderProps) {
  return (
    <header className="relative flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
      <div className="min-w-0">
        {eyebrow && <p className="text-xs font-bold uppercase tracking-[0.16em] text-brand-700">{eyebrow}</p>}
        <div className="mt-1 flex flex-wrap items-center gap-3">
          <h1 className="text-2xl font-bold tracking-tight text-ink md:text-[2rem] md:leading-tight">{title}</h1>
          {badge}
        </div>
        {description && <p className="mt-2 max-w-2xl text-[15px] text-muted">{description}</p>}
      </div>
      <div className="flex shrink-0 items-end gap-6">
        {actions && <div className="flex flex-wrap gap-3">{actions}</div>}
        {illustration && <img src={illustration} alt="" className="hidden h-32 w-auto xl:block" />}
      </div>
    </header>
  );
}
