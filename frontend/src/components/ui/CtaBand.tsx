import clsx from 'clsx';
import type { ReactNode } from 'react';

interface CtaBandProps {
  title: string;
  description: ReactNode;
  action: ReactNode;
  image?: string;
  tone?: 'light' | 'dark';
}

/** Full-width call to action with an optional illustration on the left. */
export function CtaBand({ title, description, action, image, tone = 'light' }: CtaBandProps) {
  return (
    <section
      className={clsx(
        'relative flex flex-col items-start gap-5 overflow-hidden rounded-card px-6 py-6 sm:flex-row sm:items-center sm:gap-8 sm:px-8',
        tone === 'light' ? 'border border-brand-100 bg-cta-band' : 'bg-gradient-to-r from-brand-800 via-brand-700 to-accent-700 text-white',
      )}
    >
      {image && <img src={image} alt="" className="hidden h-24 w-auto shrink-0 sm:block" />}
      <div className="min-w-0 flex-1">
        <h2 className={clsx('text-xl font-bold', tone === 'light' ? 'text-ink' : 'text-white')}>{title}</h2>
        <p className={clsx('mt-1 max-w-xl text-sm', tone === 'light' ? 'text-muted' : 'text-white/85')}>{description}</p>
      </div>
      <div className="shrink-0">{action}</div>
    </section>
  );
}
