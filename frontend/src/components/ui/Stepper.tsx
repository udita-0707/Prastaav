import clsx from 'clsx';
import { Check } from 'lucide-react';

interface StepperProps {
  steps: string[];
  /** Zero-based index of the current step. */
  current: number;
  /** Steps before the current one are buttons; later steps open only through Next, which validates. */
  onStepClick?: (index: number) => void;
}

export function Stepper({ steps, current, onStepClick }: StepperProps) {
  return (
    <nav aria-label="Progress">
      <ol className="flex flex-wrap items-center gap-x-3 gap-y-3">
        {steps.map((label, index) => {
          const state = index < current ? 'done' : index === current ? 'current' : 'pending';
          const marker = (
            <>
              <span
                className={clsx(
                  'flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-sm font-bold transition-colors',
                  state === 'done' && 'bg-brand-100 text-brand-700',
                  state === 'current' && 'bg-brand-600 text-white shadow-focus',
                  state === 'pending' && 'bg-slate-100 text-slate-500',
                )}
                aria-hidden
              >
                {state === 'done' ? <Check className="h-4 w-4" /> : index + 1}
              </span>
              <span className={clsx('text-sm font-semibold', state === 'current' ? 'text-brand-700' : state === 'done' ? 'text-ink' : 'text-muted')}>
                {label}
              </span>
              <span className="sr-only">{state === 'done' ? '(completed)' : state === 'current' ? '(current step)' : '(not started)'}</span>
            </>
          );
          return (
            <li key={label} className="flex items-center gap-3" aria-current={state === 'current' ? 'step' : undefined}>
              {state === 'done' && onStepClick ? (
                <button type="button" onClick={() => onStepClick(index)} className="flex items-center gap-3 rounded-full pr-2">
                  {marker}
                </button>
              ) : (
                <span className="flex items-center gap-3">{marker}</span>
              )}
              {index < steps.length - 1 && (
                <span aria-hidden className={clsx('hidden h-0.5 w-10 rounded-full lg:block xl:w-16', index < current ? 'bg-brand-300' : 'bg-slate-200')} />
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
