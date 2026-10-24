import { useQuery } from '@tanstack/react-query';
import { ArrowRight, CheckCircle2, FileText, FilePen, MessageSquareWarning, Plus, Send } from 'lucide-react';
import { Link } from 'react-router-dom';
import { errorMessage } from '../../api/client';
import { getProjectCounts, listProjects } from '../../api/endpoints';
import type { UserResponse } from '../../api/types';
import { illustrations } from '../../assets';
import { ErrorNote } from '../../components/ErrorNote';
import { ButtonLink, Card, CardHeader, CtaBand, EmptyState, PageHeader, StatCard } from '../../components/ui';
import { greeting } from '../../lib/format';
import { firstName } from '../../lib/people';
import { RecentProjectsTable } from './RecentProjectsTable';

export function StudentHome({ user }: { user: UserResponse }) {
  const counts = useQuery({ queryKey: ['projects', 'summary'], queryFn: getProjectCounts });
  const recent = useQuery({ queryKey: ['projects', 'list', { page: 0, size: 5 }], queryFn: () => listProjects({ page: 0, size: 5, sort: 'LATEST' }) });
  const changes = useQuery({
    queryKey: ['projects', 'list', { status: ['REVISION_REQUESTED'] }],
    queryFn: () => listProjects({ status: ['REVISION_REQUESTED'], sort: 'OLDEST' }),
    enabled: (counts.data?.byStatus.REVISION_REQUESTED ?? 0) > 0,
  });
  const by = counts.data?.byStatus;
  const isEmpty = counts.data?.total === 0;

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Welcome back"
        title={
          <>
            {greeting()}, {firstName(user.name)}{' '}
            <span aria-hidden className="inline-block">
              👋
            </span>
          </>
        }
        description="Turn your ideas into real-world impact."
        actions={
          <ButtonLink to="/app/projects/new" icon={<Plus className="h-4 w-4" aria-hidden />}>
            New Project
          </ButtonLink>
        }
        illustration={illustrations.studentLaptop}
      />

      <ErrorNote message={counts.error ? errorMessage(counts.error) : null} onRetry={() => counts.refetch()} />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Total projects" value={counts.data?.total} icon={<FileText className="h-6 w-6" />} tone="blue" loading={counts.isLoading} to="/app/projects" />
        <StatCard label="Drafts" value={by?.DRAFT} icon={<FilePen className="h-6 w-6" />} tone="amber" loading={counts.isLoading} to="/app/projects?status=DRAFT" />
        <StatCard
          label="Under review"
          value={by ? by.SUBMITTED + by.UNDER_REVIEW : undefined}
          hint="Submitted or with a reviewer"
          icon={<Send className="h-6 w-6" />}
          tone="violet"
          loading={counts.isLoading}
          to="/app/projects?status=SUBMITTED,UNDER_REVIEW"
        />
        <StatCard label="Approved" value={by?.APPROVED} icon={<CheckCircle2 className="h-6 w-6" />} tone="green" loading={counts.isLoading} to="/app/projects?status=APPROVED" />
      </div>

      {changes.data && changes.data.items.length > 0 && (
        <div className="flex flex-col gap-3 rounded-card border border-rose-200 bg-rose-50 p-5 sm:flex-row sm:items-center" role="status">
          <MessageSquareWarning className="h-6 w-6 shrink-0 text-rose-600" aria-hidden />
          <div className="min-w-0 flex-1">
            <p className="font-semibold text-rose-900">
              {changes.data.items.length === 1 ? 'Your reviewer requested changes' : `Reviewers requested changes on ${changes.data.items.length} projects`}
            </p>
            <ul className="mt-1 space-y-0.5 text-sm text-rose-800">
              {changes.data.items.map((p) => (
                <li key={p.id}>
                  <Link to={`/app/projects/${p.id}`} className="inline-flex items-center gap-1 font-medium underline-offset-2 hover:underline">
                    {p.title}
                    <ArrowRight className="h-3.5 w-3.5" aria-hidden />
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}

      <Card className="p-5 sm:p-6">
        <CardHeader
          title="Recent projects"
          action={
            !isEmpty && (
              <Link to="/app/projects" className="inline-flex items-center gap-1 text-sm font-semibold text-accent-700 hover:underline">
                View all projects <ArrowRight className="h-4 w-4" aria-hidden />
              </Link>
            )
          }
        />
        <div className="mt-5">
          <ErrorNote message={recent.error ? errorMessage(recent.error) : null} onRetry={() => recent.refetch()} />
          {isEmpty ? (
            <EmptyState
              title="No projects yet"
              description="Create your first project to start a proposal. You can save an incomplete draft and finish it later."
              action={
                <ButtonLink to="/app/projects/new" icon={<Plus className="h-4 w-4" aria-hidden />}>
                  Create project
                </ButtonLink>
              }
            />
          ) : (
            <RecentProjectsTable rows={recent.data?.items} loading={recent.isLoading} />
          )}
        </div>
      </Card>

      <CtaBand
        image={illustrations.ideaBulb}
        title="Have a new idea?"
        description="Start a new project proposal and turn your idea into something real."
        action={
          <ButtonLink to="/app/projects/new" icon={<Plus className="h-4 w-4" aria-hidden />} trailingIcon={<ArrowRight className="h-4 w-4" aria-hidden />}>
            Create new project
          </ButtonLink>
        }
      />
    </div>
  );
}
