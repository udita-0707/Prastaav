import type { ProjectStatus } from '../api/types';

const STEPS = ['Draft', 'Submitted', 'Under review', 'Decision'] as const;

function position(status: ProjectStatus): number {
  switch (status) {
    case 'DRAFT':
      return 0;
    case 'SUBMITTED':
      return 1;
    case 'UNDER_REVIEW':
      return 2;
    default:
      return 3;
  }
}

function decisionLabel(status: ProjectStatus): string {
  if (status === 'APPROVED') return 'Approved';
  if (status === 'REJECTED') return 'Rejected';
  if (status === 'REVISION_REQUESTED') return 'Revision requested';
  return 'Decision';
}

/** Where the project sits in its lifecycle. A revision loops back to "Under review" on resubmission. */
export function WorkflowRail({ status }: { status: ProjectStatus }) {
  const current = position(status);
  return (
    <ol className={`rail rail-${status.toLowerCase()}`} aria-label="Workflow progress">
      {STEPS.map((step, index) => {
        const state = index < current ? 'done' : index === current ? 'current' : 'todo';
        const label = index === 3 ? decisionLabel(status) : step;
        return (
          <li key={step} className={`rail-step rail-${state}`} aria-current={state === 'current' ? 'step' : undefined}>
            <span className="rail-dot" />
            <span className="rail-label">{label}</span>
          </li>
        );
      })}
    </ol>
  );
}
