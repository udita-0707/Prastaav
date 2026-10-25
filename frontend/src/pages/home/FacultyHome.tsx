import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { ArrowRight, ChevronRight, ClipboardList, Clock3, Filter, FolderOpen, MessageSquareWarning } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { errorMessage } from '../../api/client';
import { getProjectCounts, listProjects } from '../../api/endpoints';
import type { ProjectStatus, ProjectSummaryResponse, UserResponse } from '../../api/types';
import { illustrations } from '../../assets';
import { ErrorNote } from '../../components/ErrorNote';
import { STATUS_LABEL } from '../../components/labels';
import { ButtonLink, Card, CardHeader, DataTable, EmptyState, FilterBar, SearchField, Select, Skeleton, StatCard, type Column } from '../../components/ui';
import { waitingLabel } from '../../lib/format';
import { useDebouncedValue } from '../../lib/useDebouncedValue';
import { projectColumns } from '../projects/columns';

/** Statuses a faculty member can meet: projects reach them only once assigned. */
const FACULTY_STATUSES: ProjectStatus[] = ['UNDER_REVIEW', 'REVISION_REQUESTED', 'APPROVED', 'REJECTED'];

export function FacultyHome({ user }: { user: UserResponse }) {
  const counts = useQuery({ queryKey: ['projects', 'summary'], queryFn: getProjectCounts });
  const [status, setStatus] = useState('');
  const [search, setSearch] = useState('');
  const q = useDebouncedValue(search.trim());
  const listParams = { status: status ? [status as ProjectStatus] : [], q: q || undefined, page: 0, size: 6, sort: 'LATEST' as const };
  const assigned = useQuery({ queryKey: ['projects', 'list', listParams], queryFn: () => listProjects(listParams), placeholderData: keepPreviousData });
  const waiting = useQuery({
    queryKey: ['projects', 'list', { status: ['UNDER_REVIEW'], sort: 'OLDEST', page: 0, size: 5 }],
    queryFn: () => listProjects({ status: ['UNDER_REVIEW'], sort: 'OLDEST', page: 0, size: 5 }),
  });

  const columns: Column<ProjectSummaryResponse>[] = [
    projectColumns.title(),
    projectColumns.team(),
    projectColumns.status(),
    {
      key: 'actions',
      header: 'Actions',
      className: 'text-right',
      cell: (p) =>
        p.status === 'UNDER_REVIEW' ? (
          <ButtonLink to={`/app/projects/${p.id}#review`} size="sm" trailingIcon={<ArrowRight className="h-3.5 w-3.5" aria-hidden />}>
            Review
          </ButtonLink>
        ) : (
          <ButtonLink to={`/app/projects/${p.id}`} size="sm" variant="outline">
            View
          </ButtonLink>
        ),
    },
  ];

  return (
    <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_20rem]">
      <div className="min-w-0 space-y-6">
        <section className="relative overflow-hidden rounded-card border border-line/80 bg-gradient-to-r from-white via-white to-brand-50 p-6 shadow-card sm:p-8">
          <div className="relative z-10 max-w-xl">
            <h1 className="text-2xl font-bold tracking-tight text-ink sm:text-[2rem]">
              <span aria-hidden className="mr-2">
                👋
              </span>
              Welcome back, {user.name}
            </h1>
            <p className="mt-2 text-muted">Here are the projects assigned to you for review.</p>
          </div>
          <img src={illustrations.welcomeBanner} alt="" className="pointer-events-none absolute -bottom-2 right-0 hidden h-40 w-auto md:block" />
        </section>

        <ErrorNote message={counts.error ? errorMessage(counts.error) : null} onRetry={() => counts.refetch()} />
        <div className="grid gap-4 sm:grid-cols-3">
          <StatCard label="Assigned projects" value={counts.data?.total} icon={<FolderOpen className="h-6 w-6" />} tone="teal" loading={counts.isLoading} to="/app/projects" />
          <StatCard label="Pending reviews" value={counts.data?.byStatus.UNDER_REVIEW} icon={<Clock3 className="h-6 w-6" />} tone="amber" loading={counts.isLoading} to="/app/reviews" />
          <StatCard
            label="Awaiting student changes"
            value={counts.data?.byStatus.REVISION_REQUESTED}
            icon={<MessageSquareWarning className="h-6 w-6" />}
            tone="violet"
            loading={counts.isLoading}
            to="/app/projects?status=REVISION_REQUESTED"
          />
        </div>

        <Card className="space-y-5 p-5 sm:p-6">
          <CardHeader
            icon={<ClipboardList className="h-6 w-6" aria-hidden />}
            title="Assigned projects"
            description="Review and give feedback on the projects assigned to you."
            action={
              <Link to="/app/projects" className="inline-flex items-center gap-1 text-sm font-semibold text-accent-700 hover:underline">
                View all <ArrowRight className="h-4 w-4" aria-hidden />
              </Link>
            }
          />
          <FilterBar>
            <SearchField value={search} onChange={setSearch} placeholder="Search projects, students or keywords…" label="Search assigned projects" />
            <Select aria-label="Filter by status" icon={<Filter className="h-4 w-4" />} value={status} onChange={(e) => setStatus(e.target.value)} wrapperClassName="md:w-52">
              <option value="">All statuses</option>
              {FACULTY_STATUSES.map((s) => (
                <option key={s} value={s}>
                  {STATUS_LABEL[s]}
                </option>
              ))}
            </Select>
          </FilterBar>
          <ErrorNote message={assigned.error ? errorMessage(assigned.error) : null} onRetry={() => assigned.refetch()} />
          <DataTable
            caption="Assigned projects"
            minWidth="600px"
            columns={columns}
            rows={assigned.data?.items}
            rowKey={(p) => p.id}
            loading={assigned.isLoading}
            empty={
              <EmptyState
                title={status || q ? 'No projects match' : 'No projects assigned yet'}
                description={status || q ? 'Try a different search or status.' : 'The coordinator assigns submitted proposals to faculty. They will appear here.'}
              />
            }
          />
        </Card>
      </div>

      <aside className="space-y-6">
        <Card className="p-5">
          <CardHeader
            icon={<Clock3 className="h-5 w-5" aria-hidden />}
            title="Awaiting your review"
            action={
              <Link to="/app/reviews" className="text-sm font-semibold text-accent-700 hover:underline">
                View all
              </Link>
            }
          />
          <ul className="mt-4 space-y-3">
            {waiting.isLoading && [0, 1, 2].map((i) => <Skeleton key={i} className="h-16 w-full" />)}
            {waiting.data?.items.map((p) => (
              <li key={p.id}>
                <Link to={`/app/projects/${p.id}#review`} className="flex items-center gap-3 rounded-xl border border-line/80 p-3 transition-colors hover:border-brand-200 hover:bg-brand-50/40">
                  <span className="flex h-12 w-12 shrink-0 flex-col items-center justify-center rounded-xl bg-violet-50 text-violet-700">
                    <Clock3 className="h-5 w-5" aria-hidden />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-sm font-semibold text-ink">{p.title}</span>
                    <span className="mt-0.5 flex flex-wrap items-center gap-x-2 text-xs text-muted">
                      {p.owner.name}
                      <span className="font-semibold text-violet-700">{waitingLabel(p.updatedAt)}</span>
                    </span>
                  </span>
                  <ChevronRight className="h-4 w-4 shrink-0 text-slate-400" aria-hidden />
                </Link>
              </li>
            ))}
            {waiting.data?.items.length === 0 && <li className="rounded-xl bg-slate-50 p-4 text-sm text-muted">Nothing is waiting for your review.</li>}
          </ul>
        </Card>

        <figure className="relative overflow-hidden rounded-card border border-line/80 shadow-card">
          <img src={illustrations.mountainLake} alt="" className="h-72 w-full object-cover" />
          <div className="absolute inset-x-0 top-0 bg-gradient-to-b from-white via-white/85 to-transparent p-6 pb-16">
            <blockquote className="text-xl font-bold leading-snug text-ink">“Great ideas deserve great guidance.”</blockquote>
            <figcaption className="mt-2 text-sm text-slate-600">Your feedback helps turn students’ ideas into real-world impact.</figcaption>
          </div>
        </figure>
      </aside>
    </div>
  );
}
