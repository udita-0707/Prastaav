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

function parseYear(raw: string | null): number | undefined {
  const year = Number(raw);
  return raw && Number.isInteger(year) && year > 0 ? year : undefined;
}

interface Defaults {
  status?: ProjectStatus[];
  sort?: ProjectSort;
}

type Update = (patch: Record<string, string | undefined>) => void;

/** A text box whose value is written to the URL after a short pause, and follows the URL when something else changes it. */
function useDebouncedParam(urlValue: string, key: string, update: Update, normalize: (value: string) => string) {
  const [value, setValue] = useState(urlValue);
  const pushed = useRef(urlValue);
  const debounced = useDebouncedValue(value);
  useEffect(() => {
    if (urlValue !== pushed.current) {
      pushed.current = urlValue;
      setValue(urlValue);
    }
  }, [urlValue]);
  useEffect(() => {
    const next = normalize(debounced);
    if (next !== pushed.current) {
      pushed.current = next;
      update({ [key]: next || undefined });
    }
  }, [debounced]);
  return [value, setValue] as const;
}

/**
 * List filters live in the URL (status, q, reviewerId, admissionYear, branch, sort, page) so a filtered list can be
 * shared or bookmarked. The search and branch boxes update the URL after a short pause; any other change goes back
 * to the first page.
 */
export function useProjectListParams(defaults: Defaults = {}) {
  const [searchParams, setSearchParams] = useSearchParams();
  const urlQ = searchParams.get('q') ?? '';
  const urlBranch = searchParams.get('branch') ?? '';
  const statuses = searchParams.has('status') ? parseStatuses(searchParams.get('status')) : defaults.status ?? [];
  const sortParam = searchParams.get('sort') as ProjectSort | null;
  const params: ProjectListParams = {
    status: statuses,
    q: urlQ || undefined,
    reviewerId: searchParams.get('reviewerId') || undefined,
    admissionYear: parseYear(searchParams.get('admissionYear')),
    branch: urlBranch || undefined,
    sort: sortParam && SORTS.includes(sortParam) ? sortParam : defaults.sort ?? 'LATEST',
    page: Math.max(0, Number(searchParams.get('page') ?? 0) || 0),
    size: PAGE_SIZE,
  };

  const update: Update = (patch) => {
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

  // The text inputs are local so typing stays smooth.
  const [search, setSearch] = useDebouncedParam(urlQ, 'q', update, (value) => value.trim());
  const [branch, setBranch] = useDebouncedParam(urlBranch, 'branch', update, (value) => value.trim().toUpperCase());

  return {
    params,
    search,
    setSearch,
    branch,
    setBranch,
    setStatus: (value: string) => update({ status: value || undefined }),
    setReviewer: (value: string) => update({ reviewerId: value || undefined }),
    setAdmissionYear: (value: string) => update({ admissionYear: value || undefined }),
    setSort: (value: ProjectSort) => update({ sort: value === (defaults.sort ?? 'LATEST') ? undefined : value }),
    setPage: (page: number) => update({ page: page > 0 ? String(page) : undefined }),
  };
}
