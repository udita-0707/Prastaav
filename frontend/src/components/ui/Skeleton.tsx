import clsx from 'clsx';

export function Skeleton({ className }: { className?: string }) {
  return <span aria-hidden className={clsx('block animate-pulse rounded-lg bg-slate-200/70', className)} />;
}
