import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import clsx from 'clsx';
import { ArrowLeft, CalendarDays, CheckCircle2, CircleSlash, FilePlus2, GitCommitVertical, Hourglass, MessageSquareWarning, PencilLine, RotateCcw, Send } from 'lucide-react';
import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { createNextVersion, getHistory, getProject, listReviews, listVersions, submitProject } from '../api/endpoints';
import type { ProjectDetailResponse, ProjectStatus, ReviewResponse, TimelineEntry } from '../api/types';
import { AssignReviewerPanel } from '../components/AssignReviewerPanel';
import { ErrorNote } from '../components/ErrorNote';
import { DECISION_LABEL, FIELD_LABEL, STATUS_LABEL } from '../components/labels';
import { ProposalView } from '../components/ProposalFields';
import { ReviewPanel } from '../components/ReviewPanel';
import { Avatar, AvatarStack, Button, ButtonLink, Card, EmptyState, Modal, Skeleton, StatusPill, TabPanel, Tabs, useToast } from '../components/ui';
import { WorkflowRail } from '../components/WorkflowRail';
import { formatDateTime, formatDay } from '../lib/format';
import { parseTeam } from '../lib/people';
import { ProjectIcon } from '../lib/projectIcon';
import { useDocumentTitle } from '../lib/useDocumentTitle';

type TabId = 'overview' | 'versions' | 'reviews' | 'timeline';

/** What to tell someone who has no action available right now. */
function waitingMessage(project: ProjectDetailResponse): string {
  switch (project.status) {
    case 'SUBMITTED':
      return 'Waiting for the coordinator to assign a reviewer.';
    case 'UNDER_REVIEW':
      return `Waiting for ${project.reviewer?.name ?? 'the reviewer'} to decide.`;
    case 'REVISION_REQUESTED':
      return 'Waiting for the student to submit a revised version.';
    case 'APPROVED':
      return 'This project is approved. No further steps.';
    case 'REJECTED':
      return 'This project was rejected. No further steps.';
    default:
      return 'The student is still drafting the proposal.';
  }
}

const DECISION_STYLE: Record<ReviewResponse['decision'], { icon: typeof CheckCircle2; className: string }> = {
  APPROVE: { icon: CheckCircle2, className: 'bg-green-50 text-green-700' },
  REQUEST_REVISION: { icon: MessageSquareWarning, className: 'bg-rose-50 text-rose-700' },
  REJECT: { icon: CircleSlash, className: 'bg-slate-100 text-slate-700' },
};

/** Timeline summaries use status names ("DRAFT -> SUBMITTED"); show them with the same labels as the pills. */
function describe(entry: TimelineEntry): string {
  if (entry.type !== 'STATUS_CHANGED') return entry.type === 'REVIEW' ? entry.summary.replace(/^(APPROVE|REQUEST_REVISION|REJECT)/, (d) => DECISION_LABEL[d as ReviewResponse['decision']]) : entry.summary;
  return entry.summary
    .replace(/\b(DRAFT|SUBMITTED|UNDER_REVIEW|REVISION_REQUESTED|APPROVED|REJECTED)\b/g, (s) => STATUS_LABEL[s as ProjectStatus])
    .replace(' -> ', ' → ');
}

const TIMELINE_ICON = { VERSION_CREATED: FilePlus2, STATUS_CHANGED: GitCommitVertical, REVIEW: MessageSquareWarning } as const;

function ReviewList({ reviews }: { reviews: ReviewResponse[] }) {
  if (reviews.length === 0) return <EmptyState title="No reviews yet" description="The assigned faculty member’s decisions and comments will appear here." />;
  return (
    <ol className="space-y-4">
      {reviews
        .slice()
        .reverse()
        .map((r) => {
          const { icon: Icon, className } = DECISION_STYLE[r.decision];
          return (
            <li key={r.id} className="rounded-xl border border-line/80 p-4">
              <div className="flex flex-wrap items-center gap-3">
                <span className={clsx('inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-semibold', className)}>
                  <Icon className="h-3.5 w-3.5" aria-hidden />
                  {DECISION_LABEL[r.decision]} · v{r.versionNumber}
                </span>
                <span className="flex items-center gap-2 text-sm text-muted">
                  <Avatar name={r.reviewer.name} size="xs" />
                  {r.reviewer.name} · {formatDateTime(r.createdAt)}
                </span>
              </div>
              {r.comments && <p className="mt-3 whitespace-pre-line text-sm leading-relaxed text-slate-700">{r.comments}</p>}
            </li>
          );
        })}
    </ol>
  );
}

export function ProjectDetailPage() {
  const { id = '' } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const toast = useToast();
  const project = useQuery({ queryKey: ['project', id], queryFn: () => getProject(id) });
  const versions = useQuery({ queryKey: ['versions', id, project.data?.updatedAt], queryFn: () => listVersions(id), enabled: project.isSuccess });
  const reviews = useQuery({ queryKey: ['reviews', id], queryFn: () => listReviews(id), enabled: project.isSuccess });
  const [tab, setTab] = useState<TabId>('overview');
  const history = useQuery({ queryKey: ['history', id, project.data?.updatedAt], queryFn: () => getHistory(id), enabled: project.isSuccess && tab === 'timeline' });
  const [selectedVersion, setSelectedVersion] = useState<number | null>(null);
  const [confirmSubmit, setConfirmSubmit] = useState(false);
  useDocumentTitle(project.data?.title ?? 'Project');

  // "Review" buttons link to #review; bring the decision form into view once it exists.
  useEffect(() => {
    if (location.hash === '#review' && project.data) document.getElementById('review')?.scrollIntoView({ block: 'center' });
  }, [location.hash, project.data]);

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['project', id] });
    queryClient.invalidateQueries({ queryKey: ['projects'] });
  };
  const submit = useMutation({
    mutationFn: () => submitProject(id),
    onSuccess: (p) => {
      refresh();
      setConfirmSubmit(false);
      toast.show({ tone: 'success', title: p.status === 'UNDER_REVIEW' ? 'Revision resubmitted' : 'Proposal submitted' });
    },
    onError: () => setConfirmSubmit(false),
  });
  const revise = useMutation({
    mutationFn: () => createNextVersion(id),
    onSuccess: () => {
      refresh();
      navigate(`/app/projects/${id}/edit?step=2`);
    },
  });

  if (project.isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-28 w-full" />
        <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_22rem]">
          <Skeleton className="h-96 w-full" />
          <Skeleton className="h-64 w-full" />
        </div>
      </div>
    );
  }
  if (project.error || !project.data) return <ErrorNote message={errorMessage(project.error)} onRetry={() => project.refetch()} />;

  const p = project.data;
  const allowed = p.allowedActions;
  const current = p.currentProposal;
  const shown = versions.data?.find((v) => v.versionNumber === selectedVersion) ?? versions.data?.at(-1) ?? current ?? null;
  const reviewActions = allowed.filter((a) => a === 'APPROVE' || a === 'REQUEST_REVISION' || a === 'REJECT');
  const actionError = submit.error ?? revise.error;
  const team = parseTeam(p.teamMembers);
  const canSubmit = allowed.includes('SUBMIT') && current?.state === 'DRAFT';
  const canEdit = allowed.includes('EDIT_DRAFT') && current?.state === 'DRAFT';

  return (
    <div className="space-y-6">
      <Link to="/app/projects" className="inline-flex items-center gap-1.5 text-sm font-semibold text-muted hover:text-ink">
        <ArrowLeft className="h-4 w-4" aria-hidden /> Back to projects
      </Link>

      <Card className="p-5 sm:p-7">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
          <div className="flex min-w-0 items-start gap-4">
            <ProjectIcon id={p.id} />
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-3">
                <h1 className="text-2xl font-bold tracking-tight text-ink sm:text-[1.75rem]">{p.title}</h1>
                <StatusPill status={p.status} />
              </div>
              <p className="mt-1.5 max-w-3xl text-muted">{p.description}</p>
            </div>
          </div>
        </div>
        <dl className="mt-6 grid gap-4 border-t border-line/80 pt-5 text-sm sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <dt className="text-xs font-semibold uppercase tracking-wide text-muted">Student</dt>
            <dd className="mt-1.5 flex items-center gap-2 font-semibold text-ink">
              <Avatar name={p.owner.name} size="sm" /> {p.owner.name}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-semibold uppercase tracking-wide text-muted">Reviewer</dt>
            <dd className="mt-1.5 flex items-center gap-2 font-semibold text-ink">
              {p.reviewer ? (
                <>
                  <Avatar name={p.reviewer.name} size="sm" /> {p.reviewer.name}
                </>
              ) : (
                <span className="font-normal text-muted">Not assigned</span>
              )}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-semibold uppercase tracking-wide text-muted">Created</dt>
            <dd className="mt-1.5 flex items-center gap-2 text-ink">
              <CalendarDays className="h-4 w-4 text-slate-400" aria-hidden /> {formatDay(p.createdAt)}
            </dd>
          </div>
          <div>
            <dt className="text-xs font-semibold uppercase tracking-wide text-muted">Last updated</dt>
            <dd className="mt-1.5 flex items-center gap-2 text-ink">
              <CalendarDays className="h-4 w-4 text-slate-400" aria-hidden /> {formatDateTime(p.updatedAt)}
            </dd>
          </div>
        </dl>
        <div className="mt-6">
          <WorkflowRail status={p.status} />
        </div>
      </Card>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_22rem]">
        <Card className="min-w-0 p-5 sm:p-6">
          <Tabs
            idPrefix="project"
            label="Project sections"
            value={tab}
            onChange={(next) => setTab(next as TabId)}
            tabs={[
              { id: 'overview', label: 'Overview' },
              { id: 'versions', label: 'Versions', count: versions.data?.length },
              { id: 'reviews', label: 'Reviews', count: reviews.data?.length },
              { id: 'timeline', label: 'Timeline' },
            ]}
          />
          <div className="pt-6">
            {tab === 'overview' && (
              <TabPanel idPrefix="project" id="overview">
                <dl className="grid gap-5 sm:grid-cols-2">
                  <div>
                    <dt className="text-sm font-semibold text-ink">Tech stack</dt>
                    <dd className="mt-1 text-sm text-slate-700">{p.techStack || '—'}</dd>
                  </div>
                  <div>
                    <dt className="text-sm font-semibold text-ink">Team</dt>
                    <dd className="mt-1.5 flex flex-wrap items-center gap-3 text-sm text-slate-700">
                      <AvatarStack names={team} max={5} />
                      {team.length > 0 && <span>{team.join(', ')}</span>}
                    </dd>
                  </div>
                </dl>
                <div className="mt-8">
                  <h2 className="text-base font-bold text-ink">
                    Proposal {current ? `v${current.versionNumber}` : ''}{' '}
                    {current && <span className="text-sm font-medium text-muted">· {current.state === 'DRAFT' ? 'draft, editable by the student' : `submitted ${formatDateTime(current.submittedAt!)}, read-only`}</span>}
                  </h2>
                  <div className="mt-4">{current ? <ProposalView value={current} /> : <p className="text-sm text-muted">No proposal yet.</p>}</div>
                </div>
              </TabPanel>
            )}

            {tab === 'versions' && (
              <TabPanel idPrefix="project" id="versions">
                {versions.isLoading && <Skeleton className="h-40 w-full" />}
                <ErrorNote message={versions.error ? errorMessage(versions.error) : null} onRetry={() => versions.refetch()} />
                {versions.data && shown && (
                  <>
                    <div className="flex flex-wrap gap-2" role="group" aria-label="Choose a version">
                      {versions.data.map((v) => (
                        <button
                          key={v.id}
                          type="button"
                          aria-pressed={shown.versionNumber === v.versionNumber}
                          onClick={() => setSelectedVersion(v.versionNumber)}
                          className={clsx(
                            'rounded-full px-3.5 py-1.5 text-sm font-semibold ring-1 ring-inset transition-colors',
                            shown.versionNumber === v.versionNumber ? 'bg-brand-600 text-white ring-brand-600' : 'bg-white text-ink ring-line hover:bg-slate-50',
                          )}
                        >
                          v{v.versionNumber}
                          <span className="ml-1.5 text-xs font-medium opacity-80">{v.state === 'DRAFT' ? 'draft' : 'submitted'}</span>
                        </button>
                      ))}
                    </div>
                    <p className="mt-4 text-sm text-muted">
                      Version {shown.versionNumber} · {shown.state === 'SUBMITTED' && shown.submittedAt ? `submitted ${formatDateTime(shown.submittedAt)}, read-only` : 'draft, editable by the student'}
                    </p>
                    <div className="mt-4">
                      <ProposalView value={shown} />
                    </div>
                  </>
                )}
              </TabPanel>
            )}

            {tab === 'reviews' && (
              <TabPanel idPrefix="project" id="reviews">
                {reviews.isLoading && <Skeleton className="h-32 w-full" />}
                <ErrorNote message={reviews.error ? errorMessage(reviews.error) : null} onRetry={() => reviews.refetch()} />
                {reviews.data && <ReviewList reviews={reviews.data} />}
              </TabPanel>
            )}

            {tab === 'timeline' && (
              <TabPanel idPrefix="project" id="timeline">
                {history.isLoading && <Skeleton className="h-40 w-full" />}
                <ErrorNote message={history.error ? errorMessage(history.error) : null} onRetry={() => history.refetch()} />
                {history.data && (
                  <ol className="relative space-y-5 border-l-2 border-line pl-6">
                    {history.data.map((entry, index) => {
                      const Icon = TIMELINE_ICON[entry.type];
                      return (
                        <li key={`${entry.at}-${index}`} className="relative">
                          <span className="absolute -left-[37px] flex h-6 w-6 items-center justify-center rounded-full bg-white text-brand-600 ring-2 ring-line">
                            <Icon className="h-3.5 w-3.5" aria-hidden />
                          </span>
                          <p className="text-sm font-semibold text-ink">{describe(entry)}</p>
                          <p className="mt-0.5 text-xs text-muted">
                            {entry.actorName ?? 'Someone'} · {formatDateTime(entry.at)}
                          </p>
                        </li>
                      );
                    })}
                  </ol>
                )}
              </TabPanel>
            )}
          </div>
        </Card>

        <aside className="xl:sticky xl:top-24 xl:self-start" aria-labelledby="next-step">
          <Card className="space-y-5 p-5">
            <h2 id="next-step" className="text-lg font-bold text-ink">
              Next step
            </h2>
            {allowed.length === 0 && (
              <p className="flex items-start gap-3 rounded-xl bg-slate-50 p-4 text-sm text-slate-700">
                <Hourglass className="mt-0.5 h-4 w-4 shrink-0 text-slate-400" aria-hidden />
                {waitingMessage(p)}
              </p>
            )}

            {canEdit && (
              <div className="space-y-3">
                {current.missingFields.length > 0 && (
                  <p className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">
                    Still needed before you can submit: {current.missingFields.map((f) => FIELD_LABEL[f as keyof typeof FIELD_LABEL] ?? f).join(', ')}.
                  </p>
                )}
                <ButtonLink to={`/app/projects/${id}/edit`} variant="outline" className="w-full" icon={<PencilLine className="h-4 w-4" aria-hidden />}>
                  Edit draft v{current.versionNumber}
                </ButtonLink>
              </div>
            )}
            {canSubmit && (
              <Button variant="gradient" className="w-full" onClick={() => setConfirmSubmit(true)} icon={<Send className="h-4 w-4" aria-hidden />}>
                {p.status === 'REVISION_REQUESTED' ? `Resubmit v${current.versionNumber}` : `Submit v${current.versionNumber} for review`}
              </Button>
            )}
            {allowed.includes('CREATE_REVISION') && (
              <div className="space-y-3">
                <p className="text-sm text-slate-700">The reviewer asked for changes. Start a new version; it begins as a copy of the last one, and the submitted version stays unchanged.</p>
                <Button className="w-full" loading={revise.isPending} onClick={() => revise.mutate()} icon={<RotateCcw className="h-4 w-4" aria-hidden />}>
                  Start version {(p.latestSubmittedVersion ?? 0) + 1}
                </Button>
              </div>
            )}
            {allowed.includes('ASSIGN_REVIEWER') && <AssignReviewerPanel projectId={id} projectTitle={p.title} />}
            {reviewActions.length > 0 && <ReviewPanel projectId={id} allowed={reviewActions} versionNumber={p.latestSubmittedVersion} />}
            <ErrorNote message={actionError ? errorMessage(actionError) : null} />
          </Card>
        </aside>
      </div>

      <Modal
        open={confirmSubmit}
        onClose={() => !submit.isPending && setConfirmSubmit(false)}
        title={p.status === 'REVISION_REQUESTED' ? `Resubmit version ${current?.versionNumber}?` : `Submit version ${current?.versionNumber}?`}
        description="After submitting, this version can no longer be edited."
        footer={
          <>
            <Button variant="outline" onClick={() => setConfirmSubmit(false)} disabled={submit.isPending}>
              Cancel
            </Button>
            <Button variant="gradient" onClick={() => submit.mutate()} loading={submit.isPending} icon={<Send className="h-4 w-4" aria-hidden />}>
              Submit
            </Button>
          </>
        }
      />
    </div>
  );
}
