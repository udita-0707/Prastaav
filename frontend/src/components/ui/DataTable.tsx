import clsx from 'clsx';
import type { ReactNode } from 'react';
import { Skeleton } from './Skeleton';

export interface Column<T> {
  key: string;
  header: ReactNode;
  cell: (row: T) => ReactNode;
  className?: string;
  /** Visually hide the header text (it stays available to screen readers). */
  srOnlyHeader?: boolean;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  rows: T[] | undefined;
  rowKey: (row: T) => string;
  loading?: boolean;
  empty?: ReactNode;
  caption: string;
  /** Limits the height so the sticky header has something to stick to. */
  maxHeight?: string;
  /** Below this width the table scrolls sideways instead of squeezing columns. */
  minWidth?: string;
}

/** Sticky header, horizontal scroll on narrow screens, skeleton rows while loading. */
export function DataTable<T>({ columns, rows, rowKey, loading, empty, caption, maxHeight, minWidth = '720px' }: DataTableProps<T>) {
  if (!loading && rows && rows.length === 0 && empty) return <>{empty}</>;
  return (
    <div className="overflow-auto rounded-xl border border-line/80" style={maxHeight ? { maxHeight } : undefined}>
      <table className="w-full border-collapse text-left text-sm" style={{ minWidth }}>
        <caption className="sr-only">{caption}</caption>
        <thead className="sticky top-0 z-10 bg-slate-50/95 backdrop-blur">
          <tr>
            {columns.map((column) => (
              <th key={column.key} scope="col" className={clsx('px-4 py-3 text-xs font-semibold text-muted', column.className)}>
                {column.srOnlyHeader ? <span className="sr-only">{column.header}</span> : column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-line/80 bg-white">
          {loading
            ? Array.from({ length: 4 }, (_, index) => (
                <tr key={index}>
                  {columns.map((column) => (
                    <td key={column.key} className="px-4 py-4">
                      <Skeleton className="h-4 w-full max-w-[10rem]" />
                    </td>
                  ))}
                </tr>
              ))
            : rows?.map((row) => (
                <tr key={rowKey(row)} className="transition-colors hover:bg-slate-50/70">
                  {columns.map((column) => (
                    <td key={column.key} className={clsx('px-4 py-3.5 align-middle', column.className)}>
                      {column.cell(row)}
                    </td>
                  ))}
                </tr>
              ))}
        </tbody>
      </table>
    </div>
  );
}
