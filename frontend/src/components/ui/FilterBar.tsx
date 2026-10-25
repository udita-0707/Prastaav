import clsx from 'clsx';
import { Search, X } from 'lucide-react';
import type { ReactNode } from 'react';

/** Toolbar row above a table: a search field that grows, then compact filters. Wraps on small screens. */
export function FilterBar({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div role="search" className={clsx('flex flex-col gap-3 md:flex-row md:flex-wrap md:items-center', className)}>
      {children}
    </div>
  );
}

interface SearchFieldProps {
  value: string;
  onChange: (value: string) => void;
  placeholder: string;
  label?: string;
  className?: string;
}

export function SearchField({ value, onChange, placeholder, label = 'Search', className }: SearchFieldProps) {
  return (
    <div className={clsx('relative min-w-0 md:flex-1', className)}>
      <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" aria-hidden />
      <input
        type="search"
        aria-label={label}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="h-11 w-full rounded-field border border-line bg-white pl-10 pr-9 text-sm text-ink placeholder:text-slate-400 focus:border-brand-500 focus:shadow-focus focus:outline-none [&::-webkit-search-cancel-button]:hidden"
      />
      {value && (
        <button type="button" onClick={() => onChange('')} className="absolute right-2 top-1/2 -translate-y-1/2 rounded-md p-1 text-slate-400 hover:text-ink" aria-label="Clear search">
          <X className="h-4 w-4" aria-hidden />
        </button>
      )}
    </div>
  );
}
