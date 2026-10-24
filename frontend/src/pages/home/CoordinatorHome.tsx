import { useQuery } from '@tanstack/react-query';
import { ArrowRight, CheckCircle2, FileText, UserPlus, UserSearch } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { errorMessage } from '../../api/client';
import { getProjectCounts, listProjects } from '../../api/endpoints';
import type { UserResponse } from '../../api/types';
import { illustrations } from '../../assets';
import { ErrorNote } from '../../components/ErrorNote';
import { Button, ButtonLink, Card, CardHeader, CtaBand, DataTable, EmptyState, PageHeader, Skeleton, StatCard } from '../../components/ui';
import { greeting, waitingLabel } from '../../lib/format';
import { firstName } from '../../lib/people';
import { ProjectIcon } from '../../lib/projectIcon';
import { AssignDialog, type AssignTarget } from '../projects/AssignDialog';
import { projectColumns } from '../projects/columns';

export function CoordinatorHome({ user }: { user: UserResponse }) {
  const counts = useQuery({ queryKey: ['projects', 'summary'], queryFn: getProjectCounts });
  const awaiting = useQuery({
    queryKey: ['projects', 'list', { status: ['SUBMITTED'], sort: 'OLDEST', page: 0, size: 5 }],
    queryFn: () => listProjects({ status: ['SUBMITTED'], sort: 'OLDEST', page: 0, size: 5 }),
  });
  const recent = useQuery({ queryKey: ['projects', 'list', { page: 0, size: 5 }], queryFn: () => listProjects({ page: 0, size: 5, sort: 'LATEST' }) });
  const [target, setTarget] = useState<AssignTarget | null>(null);
  const by = counts.data?.byStatus;

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Welcome back"
        title={`${greeting()}, ${firstName(user.name)}`}
        description="Every project in one place: assign reviewers and follow each decision."
        illustration={illustrations.coordinatorLaptop}
      />
      <ErrorNote message={counts.error ? errorMessage(counts.error) : null} onRetry={() => counts.refetch()} />
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Total projects" value={counts.data?.total} icon={<FileText className="h-6 w-6" />} tone="blue" loading={counts.isLoading} to="/app/projects" />
        <StatCard label="Awaiting assignment" value={counts.data?.awaitingAssignment ?? undefined} icon={<UserPlus className="h-6 w-6" />} tone="sky" loading={counts.isLoading} to="/app/assign" />
        <StatCard label="Under review" value={by?.UNDER_REVIEW} icon={<UserSearch className="h-6 w-6" />} tone="violet" loading={counts.isLoading} to="/app/reviews" />
        <StatCard label="Approved" value={by?.APPROVED} icon={<CheckCircle2 className="h-6 w-6" />} tone="green" loading={counts.isLoading} to="/app/projects?status=APPROVED" />
      </div>

      <div className="grid gap-6 xl:grid-cols-[22rem_minmax(0,1fr)]">
        <Card className="p-5">
          <CardHeader
            title="Awaiting assignment"
            action={
              <Link to="/app/assign" className="text-sm font-semibold text-accent-700 hover:underline">
                View all
              </Link>
            }
          />
          <ul className="mt-4 space-y-3">
            {awaiting.isLoading && [0, 1, 2].map((i) => <Skeleton key={i} className="h-16 w-full" />)}
            {awaiting.data?.items.map((p) => (
              <li key={p.id} className="flex items-center gap-3 rounded-xl border border-line/80 p-3">
                <ProjectIcon id={p.id} size="sm" />
                <Link to={`/app/projects/${p.id}`} className="min-w-0 flex-1">
                  <span className="block truncate text-sm font-semibold text-ink hover:text-brand-700">{p.title}</span>
                  <span className="block text-xs text-muted">
                    {p.owner.name} · {waitingLabel(p.updatedAt)}
                  </span>
                </Link>
                <Button size="sm" variant="outline" onClick={() => setTarget(p)} aria-label={`Assign faculty to ${p.title}`}>
                  Assign
                </Button>
              </li>
            ))}
            {awaiting.data?.items.length === 0 && <li className="rounded-xl bg-slate-50 p-4 text-sm text-muted">Every submitted project has a reviewer.</li>}
          </ul>
        </Card>

        <Card className="p-5 sm:p-6">
          <CardHeader
            title="Recently updated"
            action={
              <Link to="/app/projects" className="inline-flex items-center gap-1 text-sm font-semibold text-accent-700 hover:underline">
                All projects <ArrowRight className="h-4 w-4" aria-hidden />
              </Link>
            }
          />
          <div className="mt-5">
            <ErrorNote message={recent.error ? errorMessage(recent.error) : null} onRetry={() => recent.refetch()} />
            <DataTable
              caption="Recently updated projects"
              columns={[projectColumns.title(), projectColumns.status(), projectColumns.reviewer(), projectColumns.updated()]}
              rows={recent.data?.items}
              rowKey={(p) => p.id}
              loading={recent.isLoading}
              empty={<EmptyState title="No projects yet" description="Projects appear here as soon as students create them." />}
            />
          </div>
        </Card>
      </div>

      <CtaBand
        image={illustrations.assignFaculty}
        title="Need to assign faculty?"
        description="Submitted projects wait for a reviewer. Assign one and the project moves to Under Review."
        action={
          <ButtonLink to="/app/assign" icon={<UserPlus className="h-4 w-4" aria-hidden />} trailingIcon={<ArrowRight className="h-4 w-4" aria-hidden />}>
            Assign faculty
          </ButtonLink>
        }
      />
      <AssignDialog target={target} onClose={() => setTarget(null)} />
    </div>
  );
}
