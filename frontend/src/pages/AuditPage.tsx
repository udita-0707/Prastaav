import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { Activity, FolderKanban, UserRound } from 'lucide-react';
import { useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { listAuditLog, listProjects, listUsers } from '../api/endpoints';
import type { AuditAction, AuditLogResponse, AuditQueryParams } from '../api/types';
import { ErrorNote } from '../components/ErrorNote';
import { AUDIT_ACTION_LABEL } from '../components/labels';
import { Button, Card, DataTable, EmptyState, FilterBar, Input, PageHeader, Pagination, Select, type Column } from '../components/ui';
import { describeAuditEntry } from '../lib/auditDetails';
import { formatDateTime } from '../lib/format';
import { useDocumentTitle } from '../lib/useDocumentTitle';

const PAGE_SIZE = 20;
const PROJECT_OPTIONS = 50;
const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const DAY = /^\d{4}-\d{2}-\d{2}$/;
const shortId = (id: string) => id.slice(0, 8);

/** A value from the URL only counts if the API would accept it, so an old or edited link cannot break the page. */
function validDay(raw: string | null): string {
  return raw && DAY.test(raw) && !Number.isNaN(new Date(`${raw}T00:00:00`).getTime()) ? raw : '';
}

/** Coordinator: the append-only audit log, newest first. Filters and the page number live in the URL. */
export function AuditPage() {
  useDocumentTitle('Audit log');
  const [searchParams, setSearchParams] = useSearchParams();
  const rawProject = searchParams.get('projectId') ?? '';
  const rawActor = searchParams.get('actorId') ?? '';
  const rawAction = searchParams.get('action') ?? '';
  const projectId = UUID.test(rawProject) ? rawProject : '';
  const actorId = UUID.test(rawActor) ? rawActor : '';
  const action = rawAction in AUDIT_ACTION_LABEL ? (rawAction as AuditAction) : '';
  const from = validDay(searchParams.get('from'));
  const to = validDay(searchParams.get('to'));
  const page = Math.max(0, Number(searchParams.get('page') ?? 0) || 0);
  const rangeInvalid = Boolean(from && to && from > to);
  const filtered = Boolean(projectId || actorId || action || from || to);

  const update = (patch: Record<string, string>) => {
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

  // Days are chosen in the viewer's time zone; the API takes instants, and both ends are inclusive.
  const query: AuditQueryParams = {
    projectId: projectId || undefined,
    actorId: actorId || undefined,
    action: action || undefined,
    from: from ? new Date(`${from}T00:00:00`).toISOString() : undefined,
    to: to ? new Date(`${to}T23:59:59.999`).toISOString() : undefined,
    page,
    size: PAGE_SIZE,
  };
  const entries = useQuery({ queryKey: ['audit', query], queryFn: () => listAuditLog(query), placeholderData: keepPreviousData, enabled: !rangeInvalid });
  const users = useQuery({ queryKey: ['users', 'all'], queryFn: listUsers });
  const projects = useQuery({
    queryKey: ['projects', 'list', { sort: 'LATEST', page: 0, size: PROJECT_OPTIONS }],
    queryFn: () => listProjects({ sort: 'LATEST', page: 0, size: PROJECT_OPTIONS }),
  });

  // An old link can point past the end of the log: show the last page instead of an empty one.
  const totalPages = entries.data?.totalPages ?? 0;
  useEffect(() => {
    if (totalPages > 0 && page >= totalPages) update({ page: totalPages > 1 ? String(totalPages - 1) : '' });
  }, [page, totalPages]);

  const userName = (id: string) => users.data?.find((u) => u.id === id)?.name ?? `User ${shortId(id)}`;
  const projectTitle = (id: string) => projects.data?.items.find((p) => p.id === id)?.title ?? `Project ${shortId(id)}`;

  const columns: Column<AuditLogResponse>[] = [
    { key: 'when', header: 'When', className: 'whitespace-nowrap', cell: (e) => <time dateTime={e.createdAt}>{formatDateTime(e.createdAt)}</time> },
    { key: 'action', header: 'Action', className: 'whitespace-nowrap font-semibold text-ink', cell: (e) => AUDIT_ACTION_LABEL[e.action] },
    {
      key: 'project',
      header: 'Project',
      cell: (e) =>
        e.projectId ? (
          <Link to={`/app/projects/${e.projectId}`} className="font-medium text-accent-700 hover:underline">
            {projectTitle(e.projectId)}
          </Link>
        ) : (
          '—'
        ),
    },
    { key: 'actor', header: 'Actor', cell: (e) => userName(e.actorId) },
    { key: 'details', header: 'Details', cell: (e) => <span className="text-slate-700">{describeAuditEntry(e, userName) || '—'}</span> },
  ];

  return (
    <div className="space-y-6">
      <PageHeader title="Audit Log" description="Every important action, newest first. Entries cannot be changed or deleted." />
      <Card className="space-y-5 p-5 sm:p-6">
        <FilterBar className="md:!items-end">
          <Select aria-label="Filter by project" icon={<FolderKanban className="h-4 w-4" />} value={projectId} onChange={(e) => update({ projectId: e.target.value })} wrapperClassName="md:w-56" className="truncate">
            <option value="">All projects</option>
            {projectId && !projects.data?.items.some((p) => p.id === projectId) && <option value={projectId}>{projectTitle(projectId)}</option>}
            {projects.data?.items.map((p) => (
              <option key={p.id} value={p.id}>
                {p.title}
              </option>
            ))}
          </Select>
          <Select aria-label="Filter by actor" icon={<UserRound className="h-4 w-4" />} value={actorId} onChange={(e) => update({ actorId: e.target.value })} wrapperClassName="md:w-52">
            <option value="">All actors</option>
            {actorId && !users.data?.some((u) => u.id === actorId) && <option value={actorId}>{userName(actorId)}</option>}
            {users.data?.map((u) => (
              <option key={u.id} value={u.id}>
                {u.name}
              </option>
            ))}
          </Select>
          <Select aria-label="Filter by action" icon={<Activity className="h-4 w-4" />} value={action} onChange={(e) => update({ action: e.target.value })} wrapperClassName="md:w-52">
            <option value="">All actions</option>
            {Object.entries(AUDIT_ACTION_LABEL).map(([value, label]) => (
              <option key={value} value={value}>
                {label}
              </option>
            ))}
          </Select>
          <Input type="date" label="From" value={from} max={to || undefined} onChange={(e) => update({ from: e.target.value })} wrapperClassName="md:w-40" />
          <Input type="date" label="To" value={to} min={from || undefined} onChange={(e) => update({ to: e.target.value })} wrapperClassName="md:w-40" />
          {filtered && (
            <Button variant="ghost" onClick={() => setSearchParams({}, { replace: true })}>
              Clear filters
            </Button>
          )}
        </FilterBar>
        <p className="text-xs text-muted">The project filter lists the {PROJECT_OPTIONS} most recently updated projects.</p>
        <ErrorNote message={rangeInvalid ? 'The from date must not be after the to date.' : null} />
        <ErrorNote message={entries.error ? errorMessage(entries.error) : null} onRetry={() => entries.refetch()} />
        {!rangeInvalid && (
          <DataTable
            caption="Audit log entries, newest first"
            columns={columns}
            rows={entries.data?.content}
            rowKey={(e) => e.id}
            loading={entries.isLoading}
            minWidth="880px"
            empty={<EmptyState title={filtered ? 'No entries match these filters' : 'No audit entries yet'} description={filtered ? 'Try a wider date range or fewer filters.' : 'Entries appear as soon as people use the workflow.'} />}
          />
        )}
        {!rangeInvalid && entries.data && <Pagination page={entries.data.page} size={PAGE_SIZE} total={entries.data.totalElements} onChange={(next) => update({ page: next > 0 ? String(next) : '' })} />}
      </Card>
    </div>
  );
}
