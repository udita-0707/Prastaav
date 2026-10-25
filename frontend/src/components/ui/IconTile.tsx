import clsx from 'clsx';
import type { ReactNode } from 'react';

export type Tone = 'teal' | 'blue' | 'amber' | 'violet' | 'green' | 'rose' | 'sky' | 'slate';

export const TONE_TILE: Record<Tone, string> = {
  teal: 'bg-brand-50 text-brand-600',
  blue: 'bg-accent-50 text-accent-600',
  amber: 'bg-amber-50 text-amber-600',
  violet: 'bg-violet-50 text-violet-600',
  green: 'bg-green-50 text-green-600',
  rose: 'bg-rose-50 text-rose-600',
  sky: 'bg-sky-50 text-sky-600',
  slate: 'bg-slate-100 text-slate-600',
};

/** A rounded, tinted square holding an icon: used on stat cards, table rows and feature cards. */
export function IconTile({ tone = 'teal', size = 'md', children, className }: { tone?: Tone; size?: 'sm' | 'md' | 'lg'; children: ReactNode; className?: string }) {
  return (
    <span
      aria-hidden
      className={clsx(
        'inline-flex shrink-0 items-center justify-center',
        size === 'sm' && 'h-9 w-9 rounded-[10px]',
        size === 'md' && 'h-11 w-11 rounded-xl',
        size === 'lg' && 'h-14 w-14 rounded-2xl',
        TONE_TILE[tone],
        className,
      )}
    >
      {children}
    </span>
  );
}
