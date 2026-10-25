import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, ArrowRight, Check, ChevronDown, Code2, FilePlus2, MessageSquareText, RotateCcw, Save, Send, UsersRound } from 'lucide-react';
import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, useBlocker, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { createProject, getProject, listReviews, submitProject, updateDraft } from '../api/endpoints';
import type { ProjectDetailResponse, ProposalContent } from '../api/types';
import { ErrorNote } from '../components/ErrorNote';
import { EMPTY_PROPOSAL, ProposalFields, ProposalView, REQUIRED_TO_SUBMIT } from '../components/ProposalFields';
import { Button, ButtonLink, Card, EmptyState, IconTile, Input, Modal, Skeleton, Stepper, Textarea, useToast } from '../components/ui';
import { DECISION_LABEL, FIELD_LABEL } from '../components/labels';
import { formatDateTime, formatTime } from '../lib/format';
import { parseTeam } from '../lib/people';
import { useDocumentTitle } from '../lib/useDocumentTitle';

const STEPS = ['Basic Info', 'Problem & Objectives', 'Methodology & Outcomes', 'Timeline & Review'];
const STEP_FIELDS: (keyof ProposalContent)[][] = [[], ['problemStatement', 'objectives'], ['methodology', 'expectedOutcome'], ['timeline', 'referenceLink']];
const TITLE_MAX = 100;
const DESCRIPTION_MAX = 300;
const TECH_MAX = 500;
const TEAM_MAX = 10;

interface Basics {
  title: string;
  description: string;
  techStack: string;
  teamMembers: string;
}

/** A problem found before calling the API; its message is shown as is. */
class ClientValidationError extends Error {}

const EMPTY_BASICS: Basics = { title: '', description: '', techStack: '', teamMembers: '' };
type Errors = Partial<Record<keyof Basics | keyof ProposalContent, string>>;

function fromProject(p: ProjectDetailResponse): { basics: Basics; proposal: ProposalContent } {
  const v = p.currentProposal;
  return {
    basics: { title: p.title, description: p.description, techStack: p.techStack ?? '', teamMembers: p.teamMembers ?? '' },
    proposal: {
      problemStatement: v?.problemStatement ?? '',
      objectives: v?.objectives ?? '',
      methodology: v?.methodology ?? '',
      expectedOutcome: v?.expectedOutcome ?? '',
      timeline: v?.timeline ?? '',
      referenceLink: v?.referenceLink ?? '',
    },
  };
}

/** Empty optional strings go to the API as null. */
function toPayload(proposal: ProposalContent): ProposalContent {
  return { ...proposal, referenceLink: proposal.referenceLink?.trim() || null };
}

function validateBasics(b: Basics): Errors {
  const errors: Errors = {};
  if (!b.title.trim()) errors.title = 'Give the project a title.';
  else if (b.title.length > TITLE_MAX) errors.title = `Keep the title within ${TITLE_MAX} characters.`;
  if (!b.description.trim()) errors.description = 'Add a short description.';
  else if (b.description.length > DESCRIPTION_MAX) errors.description = `Keep the description within ${DESCRIPTION_MAX} characters.`;
  if (b.techStack.length > TECH_MAX) errors.techStack = `Keep the tech stack within ${TECH_MAX} characters.`;
  if (parseTeam(b.teamMembers).length > TEAM_MAX) errors.teamMembers = `List at most ${TEAM_MAX} team members.`;
  return errors;
}

function validateProposal(proposal: ProposalContent, keys: (keyof ProposalContent)[]): Errors {
  const errors: Errors = {};
  for (const key of keys) {
    const text = proposal[key]?.trim() ?? '';
    if (key === 'referenceLink') {
      if (text) {
        try {
          new URL(text);
        } catch {
          errors.referenceLink = 'Enter a full link starting with https://';
        }
      }
    } else if (REQUIRED_TO_SUBMIT.includes(key) && !text) {
      errors[key] = `${FIELD_LABEL[key]} is needed before you can submit.`;
    }
  }
  return errors;
}

/**
 * Four-step proposal wizard. Without an id it creates the project on the first save; with an id it edits the
 * open draft (the first draft or a revision). Project details are fixed after creation because the API has no
 * endpoint to change them, so step 1 is read-only when editing.
 */
export function ProjectWizardPage() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const toast = useToast();
  const [searchParams, setSearchParams] = useSearchParams();
  const step = Math.min(Math.max(Number(searchParams.get('step') ?? 1) || 1, 1), STEPS.length) - 1;

  const project = useQuery({ queryKey: ['project', id], queryFn: () => getProject(id!), enabled: isEdit });
  const isRevision = project.data?.status === 'REVISION_REQUESTED';
  const reviews = useQuery({ queryKey: ['reviews', id], queryFn: () => listReviews(id!), enabled: isEdit && isRevision });

  const [basics, setBasics] = useState<Basics>(EMPTY_BASICS);
  const [proposal, setProposal] = useState<ProposalContent>(EMPTY_PROPOSAL);
  const [snapshot, setSnapshot] = useState(() => JSON.stringify({ basics: EMPTY_BASICS, proposal: EMPTY_PROPOSAL }));
  const [errors, setErrors] = useState<Errors>({});
  const [savedAt, setSavedAt] = useState<Date | null>(() => {
    const saved = (location.state as { savedAt?: string } | null)?.savedAt;
    return saved ? new Date(saved) : null;
  });
  const [confirmSubmit, setConfirmSubmit] = useState(false);
  const [feedbackOpen, setFeedbackOpen] = useState(true);
  const loaded = useRef(false);
  const leaving = useRef(false);

  useDocumentTitle(isEdit ? (project.data ? `Edit ${project.data.title}` : 'Edit draft') : 'Create project');

  // Fill the form once from the server; later refetches must not overwrite what the student is typing.
  useEffect(() => {
    if (!project.data || loaded.current) return;
    loaded.current = true;
    const initial = fromProject(project.data);
    setBasics(initial.basics);
    setProposal(initial.proposal);
    setSnapshot(JSON.stringify(initial));
  }, [project.data]);

  const dirty = useMemo(() => JSON.stringify({ basics, proposal }) !== snapshot, [basics, proposal, snapshot]);

  const blocker = useBlocker(({ currentLocation, nextLocation }) => dirty && !leaving.current && currentLocation.pathname !== nextLocation.pathname);
  useEffect(() => {
    if (!dirty) return;
    const onBeforeUnload = (event: BeforeUnloadEvent) => event.preventDefault();
    window.addEventListener('beforeunload', onBeforeUnload);
    return () => window.removeEventListener('beforeunload', onBeforeUnload);
  }, [dirty]);

  const goToStep = (index: number) => {
    setErrors({});
    setSearchParams({ step: String(index + 1) }, { replace: true });
    window.scrollTo({ top: 0 });
  };

  /** Creates the project on the first save, then updates the open draft. Returns the project id. */
  const persist = async (): Promise<string> => {
    if (!id) {
      const basicErrors = validateBasics(basics);
      if (Object.keys(basicErrors).length > 0) {
        setErrors(basicErrors);
        if (step !== 0) goToStep(0);
        throw new ClientValidationError('Add a title and a short description on step 1 before saving.');
      }
      const created = await createProject({ ...basics, title: basics.title.trim(), description: basics.description.trim(), proposal: toPayload(proposal) });
      return created.id;
    }
    await updateDraft(id, toPayload(proposal));
    return id;
  };

  const afterSave = (projectId: string) => {
    queryClient.invalidateQueries({ queryKey: ['projects'] });
    queryClient.invalidateQueries({ queryKey: ['project', projectId] });
    queryClient.invalidateQueries({ queryKey: ['versions', projectId] });
  };

  const save = useMutation({
    mutationFn: persist,
    onSuccess: (projectId) => {
      afterSave(projectId);
      const now = new Date();
      toast.show({ tone: 'success', title: 'Draft saved' });
      if (!id) {
        leaving.current = true;
        navigate(`/app/projects/${projectId}/edit?step=${step + 1}`, { replace: true, state: { savedAt: now.toISOString() } });
        return;
      }
      setSavedAt(now);
      setSnapshot(JSON.stringify({ basics, proposal }));
    },
  });

  const submit = useMutation({
    mutationFn: async () => {
      const projectId = await persist();
      await submitProject(projectId);
      return projectId;
    },
    onSuccess: (projectId) => {
      afterSave(projectId);
      setConfirmSubmit(false);
      toast.show({ tone: 'success', title: isRevision ? 'Revision resubmitted' : 'Proposal submitted', description: 'Your project is now in the review workflow.' });
      leaving.current = true;
      navigate(`/app/projects/${projectId}`);
    },
    onError: () => setConfirmSubmit(false),
  });

  const next = () => {
    const stepErrors = step === 0 ? (isEdit ? {} : validateBasics(basics)) : validateProposal(proposal, STEP_FIELDS[step]);
    setErrors(stepErrors);
    if (Object.keys(stepErrors).length === 0) goToStep(step + 1);
  };

  const openSubmit = () => {
    const allErrors = { ...(isEdit ? {} : validateBasics(basics)), ...validateProposal(proposal, [...REQUIRED_TO_SUBMIT, 'referenceLink']) };
    setErrors(allErrors);
    if (Object.keys(allErrors).length > 0) {
      const firstBad = STEP_FIELDS.findIndex((keys, index) => (index === 0 ? Object.keys(allErrors).some((k) => k in EMPTY_BASICS) : keys.some((k) => allErrors[k])));
      if (firstBad >= 0 && firstBad !== step) {
        goToStep(firstBad);
        setErrors(allErrors);
      }
      return;
    }
    setConfirmSubmit(true);
  };

  if (isEdit && project.isLoading) {
    return (
      <Card className="space-y-6 p-8">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-10 w-full" />
        <Skeleton className="h-40 w-full" />
      </Card>
    );
  }
  if (isEdit && project.error) return <ErrorNote message={errorMessage(project.error)} onRetry={() => project.refetch()} />;

  const draft = project.data?.currentProposal;
  if (isEdit && (!project.data?.allowedActions.includes('EDIT_DRAFT') || draft?.state !== 'DRAFT')) {
    return (
      <Card>
        <EmptyState
          title="Nothing to edit"
          description="This project has no open draft. Submitted versions cannot be changed."
          action={
            <ButtonLink to={`/app/projects/${id}`} variant="outline" icon={<ArrowLeft className="h-4 w-4" aria-hidden />}>
              Back to the project
            </ButtonLink>
          }
        />
      </Card>
    );
  }

  const canSubmit = !isEdit || Boolean(project.data?.allowedActions.includes('SUBMIT'));
  const versionNumber = draft?.versionNumber ?? 1;
  const latestFeedback = reviews.data?.filter((r) => r.decision === 'REQUEST_REVISION').at(-1);
  const teamCount = parseTeam(basics.teamMembers).length;
  const busy = save.isPending || submit.isPending;
  const mutationError = save.error ?? submit.error;
  const heading = !isEdit ? 'Create New Project' : isRevision ? project.data!.title : `Edit draft v${versionNumber}`;

  return (
    <div className={isRevision ? 'grid gap-6 xl:grid-cols-[minmax(0,1fr)_20rem]' : undefined}>
      <Card className="p-5 sm:p-8">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="flex items-start gap-4">
            <IconTile tone="teal" size="lg">
              <FilePlus2 className="h-7 w-7" />
            </IconTile>
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-ink sm:text-[1.75rem]">{heading}</h1>
              <p className="mt-1 text-muted">
                {!isEdit
                  ? 'Fill in the details below to create your project proposal.'
                  : isRevision
                    ? 'Update the proposal using the reviewer’s feedback, then resubmit.'
                    : `${project.data!.title} · fields can stay blank while drafting.`}
              </p>
            </div>
          </div>
          <p className="text-sm font-medium text-muted">
            Step {step + 1} of {STEPS.length}
          </p>
        </div>

        {isRevision && (
          <div className="mt-6 flex items-center gap-3 rounded-xl border border-violet-200 bg-violet-50 px-4 py-3 text-sm text-violet-900">
            <RotateCcw className="h-4 w-4 shrink-0" aria-hidden />
            <p>
              <span className="font-bold">Revision v{versionNumber}.</span> It started as a copy of v{versionNumber - 1}; the submitted v{versionNumber - 1} stays unchanged.
            </p>
          </div>
        )}

        <div className="mt-8">
          <Stepper steps={STEPS} current={step} onStepClick={goToStep} />
        </div>

        <form
          className="mt-8"
          noValidate
          onSubmit={(e) => {
            e.preventDefault();
            if (step < STEPS.length - 1) next();
            else if (canSubmit) openSubmit();
          }}
        >
          <h2 className="sr-only">{STEPS[step]}</h2>
          {step === 0 && (
            <div className="space-y-6">
              {isEdit && <p className="rounded-xl bg-slate-50 px-4 py-3 text-sm text-muted">Project details are set when the project is created and cannot be changed here.</p>}
              <Input
                label="Project title"
                value={basics.title}
                onChange={(e) => setBasics({ ...basics, title: e.target.value })}
                placeholder="e.g. Smart Attendance System using Face Recognition"
                maxLength={TITLE_MAX}
                showCount={!isEdit}
                required
                disabled={isEdit}
                error={errors.title}
                autoFocus={!isEdit}
              />
              <Textarea
                label="Short description"
                value={basics.description}
                onChange={(e) => setBasics({ ...basics, description: e.target.value })}
                placeholder="Provide a brief description of your project (2 to 3 lines)."
                rows={4}
                maxLength={isEdit ? undefined : DESCRIPTION_MAX}
                showCount={!isEdit}
                required
                disabled={isEdit}
                error={errors.description}
              />
              <div className="grid gap-6 md:grid-cols-2">
                <Input
                  label={
                    <span className="inline-flex items-center gap-2">
                      <Code2 className="h-4 w-4 text-slate-500" aria-hidden /> Tech stack
                    </span>
                  }
                  value={basics.techStack}
                  onChange={(e) => setBasics({ ...basics, techStack: e.target.value })}
                  placeholder="e.g. Java, React, PostgreSQL"
                  maxLength={TECH_MAX}
                  disabled={isEdit}
                  error={errors.techStack}
                />
                <Input
                  label={
                    <span className="inline-flex items-center gap-2">
                      <UsersRound className="h-4 w-4 text-slate-500" aria-hidden /> Team members
                    </span>
                  }
                  value={basics.teamMembers}
                  onChange={(e) => setBasics({ ...basics, teamMembers: e.target.value })}
                  placeholder="Team member names, comma separated"
                  hint={`${teamCount}/${TEAM_MAX} members`}
                  disabled={isEdit}
                  error={errors.teamMembers}
                />
              </div>
            </div>
          )}

          {(step === 1 || step === 2) && <ProposalFields value={proposal} onChange={setProposal} fields={STEP_FIELDS[step]} errors={errors} />}

          {step === 3 && (
            <div className="space-y-8">
              <ProposalFields value={proposal} onChange={setProposal} fields={STEP_FIELDS[3]} errors={errors} />
              <section aria-labelledby="summary-heading" className="rounded-card border border-line bg-slate-50/60 p-5 sm:p-6">
                <h3 id="summary-heading" className="text-base font-bold text-ink">
                  Review your proposal
                </h3>
                <dl className="mt-4 grid gap-4 border-b border-line/80 pb-4 text-sm sm:grid-cols-2">
                  <div>
                    <dt className="font-semibold text-ink">Title</dt>
                    <dd className="mt-0.5 text-slate-700">{basics.title || <span className="italic text-muted">Not written yet</span>}</dd>
                  </div>
                  <div>
                    <dt className="font-semibold text-ink">Tech stack</dt>
                    <dd className="mt-0.5 text-slate-700">{basics.techStack || '—'}</dd>
                  </div>
                  <div className="sm:col-span-2">
                    <dt className="font-semibold text-ink">Short description</dt>
                    <dd className="mt-0.5 text-slate-700">{basics.description || <span className="italic text-muted">Not written yet</span>}</dd>
                  </div>
                  <div className="sm:col-span-2">
                    <dt className="font-semibold text-ink">Team</dt>
                    <dd className="mt-0.5 text-slate-700">{parseTeam(basics.teamMembers).join(', ') || '—'}</dd>
                  </div>
                </dl>
                <div className="mt-4">
                  <ProposalView value={proposal} />
                </div>
              </section>
            </div>
          )}

          <div className="mt-6">
            <ErrorNote message={mutationError ? (mutationError instanceof ClientValidationError ? mutationError.message : errorMessage(mutationError)) : null} />
          </div>

          <div className="mt-8 flex flex-col-reverse gap-4 border-t border-line pt-6 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-3">
              <Button variant="ghost" onClick={() => save.mutate()} loading={save.isPending} disabled={busy} icon={<Save className="h-4 w-4" aria-hidden />} className="text-accent-700">
                Save draft
              </Button>
              <span className="text-sm text-muted" aria-live="polite">
                {dirty ? 'Unsaved changes' : savedAt ? (
                  <span className="inline-flex items-center gap-1 text-green-700">
                    <Check className="h-4 w-4" aria-hidden /> Saved at {formatTime(savedAt)}
                  </span>
                ) : null}
              </span>
            </div>
            <div className="flex gap-3">
              {step > 0 && (
                <Button variant="outline" onClick={() => goToStep(step - 1)} disabled={busy} icon={<ArrowLeft className="h-4 w-4" aria-hidden />}>
                  Back
                </Button>
              )}
              {step < STEPS.length - 1 ? (
                <Button type="submit" disabled={busy} trailingIcon={<ArrowRight className="h-4 w-4" aria-hidden />}>
                  Next
                </Button>
              ) : (
                canSubmit && (
                  <Button type="submit" variant="gradient" disabled={busy} icon={<Send className="h-4 w-4" aria-hidden />}>
                    {isRevision ? `Resubmit v${versionNumber}` : 'Submit proposal'}
                  </Button>
                )
              )}
            </div>
          </div>
        </form>
      </Card>

      {isRevision && (
        <aside className="xl:sticky xl:top-24 xl:self-start" aria-label="Reviewer feedback">
          <Card className="p-5">
            <button type="button" className="flex w-full items-center justify-between gap-3 text-left" aria-expanded={feedbackOpen} onClick={() => setFeedbackOpen((o) => !o)}>
              <span className="inline-flex items-center gap-2 font-bold text-ink">
                <MessageSquareText className="h-5 w-5 text-rose-600" aria-hidden />
                Reviewer feedback
              </span>
              <ChevronDown className={`h-4 w-4 text-slate-400 transition-transform ${feedbackOpen ? 'rotate-180' : ''}`} aria-hidden />
            </button>
            {feedbackOpen && (
              <div className="mt-4 text-sm">
                {reviews.isLoading && <Skeleton className="h-20 w-full" />}
                {latestFeedback ? (
                  <>
                    <p className="text-xs font-semibold uppercase tracking-wide text-rose-700">
                      {DECISION_LABEL[latestFeedback.decision]} on v{latestFeedback.versionNumber}
                    </p>
                    <p className="mt-2 whitespace-pre-line leading-relaxed text-slate-700">{latestFeedback.comments}</p>
                    <p className="mt-3 text-xs text-muted">
                      {latestFeedback.reviewer.name} · {formatDateTime(latestFeedback.createdAt)}
                    </p>
                  </>
                ) : (
                  !reviews.isLoading && <p className="text-muted">No feedback found.</p>
                )}
                <Link to={`/app/projects/${id}`} className="mt-4 inline-block font-semibold text-accent-700 hover:underline">
                  Open the project page
                </Link>
              </div>
            )}
          </Card>
        </aside>
      )}

      <Modal
        open={confirmSubmit}
        onClose={() => !submit.isPending && setConfirmSubmit(false)}
        title={isRevision ? `Resubmit version ${versionNumber}?` : 'Submit your proposal?'}
        description={
          isRevision
            ? `Version ${versionNumber} goes back to ${project.data?.reviewer?.name ?? 'your reviewer'}. After submitting, it can no longer be edited.`
            : 'The coordinator will assign a faculty reviewer. After submitting, this version can no longer be edited.'
        }
        footer={
          <>
            <Button variant="outline" onClick={() => setConfirmSubmit(false)} disabled={submit.isPending}>
              Keep editing
            </Button>
            <Button variant="gradient" onClick={() => submit.mutate()} loading={submit.isPending} icon={<Send className="h-4 w-4" aria-hidden />}>
              {isRevision ? 'Resubmit' : 'Submit'}
            </Button>
          </>
        }
      />

      <Modal
        open={blocker.state === 'blocked'}
        onClose={() => blocker.reset?.()}
        title="Leave without saving?"
        description="You have changes that are not saved. If you leave now they will be lost."
        footer={
          <>
            <Button variant="outline" onClick={() => blocker.reset?.()}>
              Stay on this page
            </Button>
            <Button variant="danger" onClick={() => blocker.proceed?.()}>
              Leave
            </Button>
          </>
        }
      />
    </div>
  );
}
