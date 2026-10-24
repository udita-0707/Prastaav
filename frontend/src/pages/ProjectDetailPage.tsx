import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { createNextVersion, getProject, listReviews, listVersions, submitProject } from '../api/endpoints';
import type { ProjectDetailResponse } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { AssignReviewerPanel } from '../components/AssignReviewerPanel';
import { ErrorNote } from '../components/ErrorNote';
import { DECISION_LABEL, FIELD_LABEL, formatDate } from '../components/labels';
import { ProposalView } from '../components/ProposalFields';
import { ReviewPanel } from '../components/ReviewPanel';
import { StatusBadge } from '../components/StatusBadge';
import { WorkflowRail } from '../components/WorkflowRail';

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

export function ProjectDetailPage() {
  const { id = '' } = useParams();
  const { user } = useAuth();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const project = useQuery({ queryKey: ['project', id], queryFn: () => getProject(id) });
  const versions = useQuery({ queryKey: ['versions', id, project.data?.updatedAt], queryFn: () => listVersions(id) });
  const reviews = useQuery({ queryKey: ['reviews', id], queryFn: () => listReviews(id) });
  const [selectedVersion, setSelectedVersion] = useState<number | null>(null);

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['project', id] });
    queryClient.invalidateQueries({ queryKey: ['projects'] });
  };
  const submit = useMutation({ mutationFn: () => submitProject(id), onSuccess: refresh });
  const revise = useMutation({
    mutationFn: () => createNextVersion(id),
    onSuccess: () => {
      refresh();
      navigate(`/app/projects/${id}/edit`);
    },
  });

  if (project.isLoading) return <p className="muted">Loading…</p>;
  if (project.error || !project.data) return <ErrorNote message={errorMessage(project.error)} />;

  const p = project.data;
  const allowed = p.allowedActions;
  const current = p.currentProposal;
  const shown =
    versions.data?.find((v) => v.versionNumber === selectedVersion) ?? current ?? (versions.data ? versions.data[versions.data.length - 1] : undefined) ?? null;
  const reviewActions = allowed.filter((a) => a === 'APPROVE' || a === 'REQUEST_REVISION' || a === 'REJECT');
  const actionError = submit.error ?? revise.error;

  return (
    <article className="detail">
      <header className="detail-head">
        <div>
          <h1>{p.title}</h1>
          <p className="muted">
            {p.owner.name} · reviewer {p.reviewer?.name ?? 'not assigned'}
          </p>
        </div>
        <StatusBadge status={p.status} />
      </header>

      <WorkflowRail status={p.status} />

      <div className="detail-grid">
        <section className="panel next-step" aria-labelledby="next-step">
          <h2 id="next-step">Next step</h2>
          {allowed.length === 0 && <p className="muted">{waitingMessage(p)}</p>}

          {allowed.includes('EDIT_DRAFT') && current?.state === 'DRAFT' && (
            <div className="stack">
              {current.missingFields.length > 0 && (
                <p className="hint-box">
                  Still needed before you can submit: {current.missingFields.map((f) => FIELD_LABEL[f as keyof typeof FIELD_LABEL] ?? f).join(', ')}.
                </p>
              )}
              <Link to={`/app/projects/${id}/edit`} className="button">
                Edit draft v{current.versionNumber}
              </Link>
            </div>
          )}
          {allowed.includes('SUBMIT') && current?.state === 'DRAFT' && (
            <button className="primary" disabled={submit.isPending} onClick={() => submit.mutate()}>
              {p.status === 'REVISION_REQUESTED' ? `Resubmit v${current.versionNumber}` : `Submit v${current.versionNumber} for review`}
            </button>
          )}
          {allowed.includes('CREATE_REVISION') && (
            <div className="stack">
              <p>The reviewer asked for changes. Start a new version; it begins as a copy of the last one.</p>
              <button className="primary" disabled={revise.isPending} onClick={() => revise.mutate()}>
                Start version {(p.latestSubmittedVersion ?? 0) + 1}
              </button>
            </div>
          )}
          {allowed.includes('ASSIGN_REVIEWER') && <AssignReviewerPanel projectId={id} projectTitle={p.title} />}
          {reviewActions.length > 0 && (
            <ReviewPanel projectId={id} allowed={reviewActions} versionNumber={p.latestSubmittedVersion} />
          )}
          <ErrorNote message={actionError ? errorMessage(actionError) : null} />
        </section>

        <section className="panel" aria-labelledby="proposal">
          <div className="panel-head">
            <h2 id="proposal">Proposal</h2>
            {versions.data && versions.data.length > 1 && (
              <div className="version-tabs" role="tablist" aria-label="Versions">
                {versions.data.map((v) => (
                  <button
                    key={v.id}
                    role="tab"
                    aria-selected={shown?.versionNumber === v.versionNumber}
                    className={shown?.versionNumber === v.versionNumber ? 'tab active' : 'tab'}
                    onClick={() => setSelectedVersion(v.versionNumber)}
                  >
                    v{v.versionNumber}
                  </button>
                ))}
              </div>
            )}
          </div>
          {shown ? (
            <>
              <p className="muted">
                Version {shown.versionNumber} ·{' '}
                {shown.state === 'SUBMITTED' && shown.submittedAt
                  ? `submitted ${formatDate(shown.submittedAt)}, read-only`
                  : 'draft, editable by the student'}
              </p>
              <ProposalView value={shown} />
            </>
          ) : (
            <p className="muted">No proposal yet.</p>
          )}
        </section>

        <section className="panel" aria-labelledby="feedback">
          <h2 id="feedback">Reviewer feedback</h2>
          {reviews.data?.length === 0 && <p className="muted">No reviews yet.</p>}
          <ol className="reviews">
            {reviews.data
              ?.slice()
              .reverse()
              .map((r) => (
                <li key={r.id} className={`review review-${r.decision.toLowerCase()}`}>
                  <p>
                    <strong>{DECISION_LABEL[r.decision]}</strong> on v{r.versionNumber}
                    <span className="muted"> · {r.reviewer.name}, {formatDate(r.createdAt)}</span>
                  </p>
                  {r.comments && <p className="review-comments">{r.comments}</p>}
                </li>
              ))}
          </ol>
        </section>

        <section className="panel" aria-labelledby="about">
          <h2 id="about">About the project</h2>
          <p>{p.description}</p>
          <dl className="facts">
            <div>
              <dt>Tech stack</dt>
              <dd>{p.techStack || '—'}</dd>
            </div>
            <div>
              <dt>Team</dt>
              <dd>{p.teamMembers || '—'}</dd>
            </div>
            <div>
              <dt>Created</dt>
              <dd>{formatDate(p.createdAt)}</dd>
            </div>
          </dl>
          {user?.role === 'COORDINATOR' && <p className="muted">Viewing as coordinator.</p>}
        </section>
      </div>
    </article>
  );
}
