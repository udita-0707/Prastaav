import clsx from 'clsx';
import { ChevronLeft, ChevronRight } from 'lucide-react';

interface PaginationProps {
  /** Zero-based page index, matching the API. */
  page: number;
  size: number;
  total: number;
  onChange: (page: number) => void;
}

function pageWindow(current: number, count: number): (number | 'gap')[] {
  if (count <= 7) return Array.from({ length: count }, (_, i) => i);
  const pages = new Set([0, count - 1, current - 1, current, current + 1].filter((p) => p >= 0 && p < count));
  const sorted = [...pages].sort((a, b) => a - b);
  const result: (number | 'gap')[] = [];
  sorted.forEach((p, i) => {
    if (i > 0 && p - sorted[i - 1] > 1) result.push('gap');
    result.push(p);
  });
  return result;
}

export function Pagination({ page, size, total, onChange }: PaginationProps) {
  const count = Math.max(1, Math.ceil(total / size));
  if (total === 0) return null;
  const from = page * size + 1;
  const to = Math.min(total, (page + 1) * size);
  const button = 'inline-flex h-9 min-w-9 items-center justify-center rounded-lg px-2 text-sm font-semibold transition-colors';
  return (
    <nav className="flex flex-wrap items-center justify-between gap-3" aria-label="Pagination">
      <p className="text-sm text-muted">
        Showing <span className="font-semibold text-ink">{from}</span>–<span className="font-semibold text-ink">{to}</span> of{' '}
        <span className="font-semibold text-ink">{total}</span>
      </p>
      {count > 1 && (
        <div className="flex items-center gap-1">
          <button type="button" className={clsx(button, 'text-ink hover:bg-slate-100 disabled:opacity-40')} onClick={() => onChange(page - 1)} disabled={page === 0} aria-label="Previous page">
            <ChevronLeft className="h-4 w-4" aria-hidden />
          </button>
          {pageWindow(page, count).map((p, i) =>
            p === 'gap' ? (
              <span key={`gap-${i}`} className="px-1 text-muted" aria-hidden>
                …
              </span>
            ) : (
              <button
                key={p}
                type="button"
                onClick={() => onChange(p)}
                aria-current={p === page ? 'page' : undefined}
                className={clsx(button, p === page ? 'bg-brand-600 text-white' : 'text-ink hover:bg-slate-100')}
              >
                {p + 1}
              </button>
            ),
          )}
          <button type="button" className={clsx(button, 'text-ink hover:bg-slate-100 disabled:opacity-40')} onClick={() => onChange(page + 1)} disabled={page >= count - 1} aria-label="Next page">
            <ChevronRight className="h-4 w-4" aria-hidden />
          </button>
        </div>
      )}
    </nav>
  );
}
