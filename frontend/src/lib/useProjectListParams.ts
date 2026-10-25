import { useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import type { ProjectListParams, ProjectSort, ProjectStatus } from '../api/types';
import { STATUS_LABEL } from '../components/labels';
import { useDebouncedValue } from './useDebouncedValue';

export const PAGE_SIZE = 10;
const SORTS: ProjectSort[] = ['LATEST', 'OLDEST', 'TITLE'];

function parseStatuses(raw: string | null): ProjectStatus[] {
  if (!raw) return [];
  return raw.split(',').filter((s): s is ProjectStatus => s in STATUS_LABEL);
}

interface Defaults {
  status?: ProjectStatus[];
  sort?: ProjectSort;
}

/**
 * List filters live in the URL (status, q, reviewerId, sort, page) so a filtered list can be shared or bookmarked.
 * The search box updates the URL after a short pause; any other change goes back to the first page.
 */
export function useProjectListParams(defaults: Defaults = {}) {
  const [searchParams, setSearchParams] = useSearchParams();
  const urlQ = searchParams.get('q') ?? '';
  const statuses = searchParams.has('status') ? parseStatuses(searchParams.get('status')) : defaults.status ?? [];
  const sortParam = searchParams.get('sort') as ProjectSort | null;
  const params: ProjectListParams = {
    status: statuses,
    q: urlQ || undefined,
    reviewerId: searchParams.get('reviewerId') || undefined,
    sort: sortParam && SORTS.includes(sortParam) ? sortParam : defaults.sort ?? 'LATEST',
    page: Math.max(0, Number(searchParams.get('page') ?? 0) || 0),
    size: PAGE_SIZE,
  };

  const update = (patch: Record<string, string | undefined>) => {
    setSearchParams(
      (current) => {
        const next = new URLSearchParams(current);
        for (const [key, value] of Object.entries(patch)) {
          if (value) next.set(key, value);
          else next.delete(key);
        }
        if (!('page' in patch)) next.delete('page');
        return next;
      },
      { replace: true },
    );
  };

  // The search input is local so typing stays smooth; it follows the URL when something else changes q.
  const [search, setSearch] = useState(urlQ);
  const pushed = useRef(urlQ);
  const debounced = useDebouncedValue(search);
  useEffect(() => {
    if (urlQ !== pushed.current) {
      pushed.current = urlQ;
      setSearch(urlQ);
    }
  }, [urlQ]);
  useEffect(() => {
    const term = debounced.trim();
    if (term !== pushed.current) {
      pushed.current = term;
      update({ q: term || undefined });
    }
  }, [debounced]);

  return {
    params,
    search,
    setSearch,
    setStatus: (value: string) => update({ status: value || undefined }),
    setReviewer: (value: string) => update({ reviewerId: value || undefined }),
    setSort: (value: ProjectSort) => update({ sort: value === (defaults.sort ?? 'LATEST') ? undefined : value }),
    setPage: (page: number) => update({ page: page > 0 ? String(page) : undefined }),
  };
}
