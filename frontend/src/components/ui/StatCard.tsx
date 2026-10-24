import clsx from 'clsx';
import { ChevronRight } from 'lucide-react';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { IconTile, type Tone } from './IconTile';
import { Skeleton } from './Skeleton';

const GLOW: Record<Tone, string> = {
  teal: 'from-brand-50',
  blue: 'from-accent-50',
  amber: 'from-amber-50',
  violet: 'from-violet-50',
  green: 'from-green-50',
  rose: 'from-rose-50',
  sky: 'from-sky-50',
  slate: 'from-slate-50',
};

interface StatCardProps {
  label: string;
  value: number | undefined;
  icon: ReactNode;
  tone?: Tone;
  hint?: string;
  loading?: boolean;
  to?: string;
}

/** One headline number. Linked cards open the list the number counts. */
export function StatCard({ label, value, icon, tone = 'teal', hint, loading, to }: StatCardProps) {
  const body = (
    <>
      <IconTile tone={tone} size="lg">
        {icon}
      </IconTile>
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-muted">{label}</p>
        {loading ? <Skeleton className="mt-2 h-8 w-12" /> : <p className="mt-1 text-3xl font-bold tabular-nums text-ink">{value ?? 0}</p>}
        {hint && <p className="mt-1 text-xs text-muted">{hint}</p>}
      </div>
      {to && <ChevronRight className="h-5 w-5 text-slate-400 transition-transform group-hover:translate-x-0.5" aria-hidden />}
    </>
  );
  const className = clsx(
    'group relative flex items-center gap-4 overflow-hidden rounded-card border border-line/80 bg-white bg-gradient-to-tr to-white to-60% p-5 shadow-card',
    GLOW[tone],
    to && 'transition-shadow hover:shadow-pop',
  );
  return to ? (
    <Link to={to} className={className} aria-label={`${label}: ${value ?? 0}`}>
      {body}
    </Link>
  ) : (
    <div className={className}>{body}</div>
  );
}
