import { useQuery } from '@tanstack/react-query';
import { CheckCircle2, ChevronRight, CircleSlash, FilePen, FileText, MessageSquareWarning, Send, UserPlus, UserSearch } from 'lucide-react';
import { useState, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { getDashboardSummary } from '../api/endpoints';
import type { ProjectStatus } from '../api/types';
import { ErrorNote } from '../components/ErrorNote';
import { STATUS_LABEL } from '../components/labels';
import { Avatar, Button, Card, CardHeader, PageHeader, Skeleton, StatCard, type Tone } from '../components/ui';
import { waitingLabel } from '../lib/format';
import { ProjectIcon } from '../lib/projectIcon';
import { useDocumentTitle } from '../lib/useDocumentTitle';
import { AssignDialog, type AssignTarget } from './projects/AssignDialog';

/** Every status in workflow order, in the same colours as the status pills. */
const STATUS_CARDS: { status: ProjectStatus; tone: Tone; icon: ReactNode }[] = [
  { status: 'DRAFT', tone: 'amber', icon: <FilePen className="h-6 w-6" /> },
  { status: 'SUBMITTED', tone: 'sky', icon: <Send className="h-6 w-6" /> },
  { status: 'UNDER_REVIEW', tone: 'violet', icon: <UserSearch className="h-6 w-6" /> },
  { status: 'REVISION_REQUESTED', tone: 'rose', icon: <MessageSquareWarning className="h-6 w-6" /> },
  { status: 'APPROVED', tone: 'green', icon: <CheckCircle2 className="h-6 w-6" /> },
  { status: 'REJECTED', tone: 'slate', icon: <CircleSlash className="h-6 w-6" /> },
];

/** Coordinator overview: counts by status, projects waiting for a reviewer with quick assign, and reviewer workload. */
export function DashboardPage() {
  useDocumentTitle('Dashboard');
  // Under the 'projects' key, so every project mutation refreshes the dashboard too.
  const summary = useQuery({ queryKey: ['projects', 'dashboard'], queryFn: getDashboardSummary });
  const [target, setTarget] = useState<AssignTarget | null>(null);
  const data = summary.data;

  return (
    <div className="space-y-6">
      <PageHeader title="Dashboard" description="Where every project stands, what is waiting for a reviewer and how the reviews are shared out." />
      <ErrorNote message={summary.error ? errorMessage(summary.error) : null} onRetry={() => summary.refetch()} />

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Total projects" value={data?.total} icon={<FileText className="h-6 w-6" />} tone="blue" loading={summary.isLoading} to="/app/projects" />
        {STATUS_CARDS.map(({ status, tone, icon }) => (
          <StatCard key={status} label={STATUS_LABEL[status]} value={data?.byStatus[status]} icon={icon} tone={tone} loading={summary.isLoading} to={`/app/projects?status=${status}`} />
        ))}
      </div>

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_24rem]">
        <Card className="p-5 sm:p-6">
          <CardHeader title="Waiting for assignment" description="Submitted projects without a reviewer, the longest-waiting first." />
          <ul className="mt-4 space-y-3">
            {summary.isLoading && [0, 1, 2].map((i) => <Skeleton key={i} className="h-16 w-full" />)}
            {data?.awaitingAssignment.map((p) => (
              <li key={p.id} className="flex items-center gap-3 rounded-xl border border-line/80 p-3">
                <ProjectIcon id={p.id} size="sm" />
                <Link to={`/app/projects/${p.id}`} className="min-w-0 flex-1">
                  <span className="block truncate text-sm font-semibold text-ink hover:text-brand-700">{p.title}</span>
                  <span className="block text-xs text-muted">
                    {p.owner.name} · {waitingLabel(p.waitingSince)}
                  </span>
                </Link>
                <Button size="sm" onClick={() => setTarget(p)} icon={<UserPlus className="h-4 w-4" aria-hidden />} aria-label={`Assign faculty to ${p.title}`}>
                  Assign
                </Button>
              </li>
            ))}
            {data?.awaitingAssignment.length === 0 && <li className="rounded-xl bg-slate-50 p-4 text-sm text-muted">Every submitted project has a reviewer.</li>}
          </ul>
        </Card>

        <Card className="p-5 sm:p-6 xl:self-start">
          <CardHeader title="Reviewer workload" description="Projects under review per faculty member." />
          <ul className="mt-4 space-y-1">
            {summary.isLoading && [0, 1].map((i) => <Skeleton key={i} className="h-14 w-full" />)}
            {data?.reviewerWorkload.map(({ reviewer, openReviews }) => (
              <li key={reviewer.id}>
                <Link
                  to={`/app/projects?status=UNDER_REVIEW&reviewerId=${reviewer.id}`}
                  className="group flex items-center gap-3 rounded-xl p-2.5 hover:bg-slate-50"
                >
                  <Avatar name={reviewer.name} size="sm" />
                  <span className="min-w-0 flex-1 truncate text-sm font-semibold text-ink">{reviewer.name}</span>
                  <span className="text-lg font-bold tabular-nums text-ink">
                    {openReviews}
                    <span className="sr-only"> under review</span>
                  </span>
                  <ChevronRight className="h-4 w-4 text-slate-400 transition-transform group-hover:translate-x-0.5" aria-hidden />
                </Link>
              </li>
            ))}
            {data?.reviewerWorkload.length === 0 && <li className="rounded-xl bg-slate-50 p-4 text-sm text-muted">There are no faculty accounts yet.</li>}
          </ul>
        </Card>
      </div>

      <AssignDialog target={target} onClose={() => setTarget(null)} />
    </div>
  );
}
