import clsx from 'clsx';
import { Check, X } from 'lucide-react';
import type { ProjectStatus } from '../api/types';

const STEPS = ['Draft', 'Submitted', 'Under Review', 'Decision'] as const;

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

function decision(status: ProjectStatus): { label: string; className: string } {
  if (status === 'APPROVED') return { label: 'Approved', className: 'bg-green-600 text-white' };
  if (status === 'REJECTED') return { label: 'Rejected', className: 'bg-slate-600 text-white' };
  if (status === 'REVISION_REQUESTED') return { label: 'Changes Requested', className: 'bg-rose-600 text-white' };
  return { label: 'Decision', className: 'bg-brand-600 text-white' };
}

/** Where the project sits in its lifecycle. A revision loops back to "Under Review" on resubmission. */
export function WorkflowRail({ status }: { status: ProjectStatus }) {
  const current = position(status);
  const outcome = decision(status);
  return (
    <ol className="grid grid-cols-4 gap-2" aria-label="Workflow progress">
      {STEPS.map((step, index) => {
        const state = index < current ? 'done' : index === current ? 'current' : 'pending';
        const isDecision = index === 3 && state === 'current';
        const label = index === 3 ? outcome.label : step;
        return (
          <li key={step} className="flex flex-col gap-2" aria-current={state === 'current' ? 'step' : undefined}>
            <span className={clsx('h-1.5 rounded-full', state === 'pending' ? 'bg-slate-200' : isDecision && status === 'REJECTED' ? 'bg-slate-400' : isDecision && status === 'REVISION_REQUESTED' ? 'bg-rose-400' : 'bg-brand-500')} />
            <span className="flex items-center gap-2">
              <span
                className={clsx(
                  'flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-[11px] font-bold',
                  state === 'done' && 'bg-brand-100 text-brand-700',
                  state === 'current' && (isDecision ? outcome.className : 'bg-brand-600 text-white'),
                  state === 'pending' && 'bg-slate-100 text-slate-500',
                )}
                aria-hidden
              >
                {state === 'done' || (isDecision && status === 'APPROVED') ? <Check className="h-3.5 w-3.5" /> : isDecision && status === 'REJECTED' ? <X className="h-3.5 w-3.5" /> : index + 1}
              </span>
              <span className={clsx('text-xs font-semibold sm:text-sm', state === 'pending' ? 'text-muted' : 'text-ink')}>{label}</span>
            </span>
          </li>
        );
      })}
    </ol>
  );
}
