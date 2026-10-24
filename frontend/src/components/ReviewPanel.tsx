import { useMutation, useQueryClient } from '@tanstack/react-query';
import clsx from 'clsx';
import { CheckCircle2, CircleSlash, MessageSquareWarning, type LucideIcon } from 'lucide-react';
import { useState } from 'react';
import { errorMessage } from '../api/client';
import { recordReview } from '../api/endpoints';
import type { Action, ReviewDecision } from '../api/types';
import { ErrorNote } from './ErrorNote';
import { Button, Textarea, useToast } from './ui';

const OPTIONS: { decision: ReviewDecision; label: string; hint: string; icon: LucideIcon; selected: string }[] = [
  { decision: 'APPROVE', label: 'Approve', hint: 'The proposal is ready.', icon: CheckCircle2, selected: 'border-green-500 bg-green-50 text-green-800' },
  { decision: 'REQUEST_REVISION', label: 'Request changes', hint: 'The student revises and resubmits.', icon: MessageSquareWarning, selected: 'border-rose-500 bg-rose-50 text-rose-800' },
  { decision: 'REJECT', label: 'Reject', hint: 'Ends the workflow for this project.', icon: CircleSlash, selected: 'border-slate-500 bg-slate-100 text-slate-800' },
];

/** The assigned faculty member's decision on the latest submitted version. Only decisions the backend allows are offered. */
export function ReviewPanel({ projectId, allowed, versionNumber }: { projectId: string; allowed: Action[]; versionNumber: number | null }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const options = OPTIONS.filter((o) => allowed.includes(o.decision));
  const [decision, setDecision] = useState<ReviewDecision>(options[0]?.decision ?? 'APPROVE');
  const [comments, setComments] = useState('');
  const needsComments = decision !== 'APPROVE';
  const mutation = useMutation({
    mutationFn: () => recordReview(projectId, { decision, comments, versionNumber }),
    onSuccess: () => {
      setComments('');
      queryClient.invalidateQueries({ queryKey: ['project', projectId] });
      queryClient.invalidateQueries({ queryKey: ['reviews', projectId] });
      queryClient.invalidateQueries({ queryKey: ['history', projectId] });
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      toast.show({ tone: 'success', title: 'Decision recorded', description: `${OPTIONS.find((o) => o.decision === decision)?.label} on version ${versionNumber}.` });
    },
  });

  return (
    <form
      id="review"
      className="space-y-4"
      onSubmit={(e) => {
        e.preventDefault();
        mutation.mutate();
      }}
    >
      <p className="text-sm text-muted">You are reviewing version {versionNumber}.</p>
      <fieldset>
        <legend className="text-sm font-semibold text-ink">Decision</legend>
        <div className="mt-2 space-y-2">
          {options.map(({ decision: value, label, hint, icon: Icon, selected }) => (
            <label
              key={value}
              className={clsx(
                'flex cursor-pointer items-start gap-3 rounded-xl border-2 px-3.5 py-3 transition-colors focus-within:ring-2 focus-within:ring-brand-500',
                decision === value ? selected : 'border-line bg-white text-ink hover:border-slate-300',
              )}
            >
              <input type="radio" name="decision" className="sr-only" checked={decision === value} onChange={() => setDecision(value)} />
              <Icon className="mt-0.5 h-5 w-5 shrink-0" aria-hidden />
              <span>
                <span className="block text-sm font-semibold">{label}</span>
                <span className="block text-xs opacity-80">{hint}</span>
              </span>
            </label>
          ))}
        </div>
      </fieldset>
      <Textarea
        label="Comments"
        hint={needsComments ? 'Required: explain what the student should change or why.' : 'Optional.'}
        required={needsComments}
        rows={4}
        value={comments}
        onChange={(e) => setComments(e.target.value)}
      />
      <ErrorNote message={mutation.error ? errorMessage(mutation.error) : null} />
      <Button type="submit" className="w-full" loading={mutation.isPending} disabled={needsComments && !comments.trim()}>
        Record decision
      </Button>
    </form>
  );
}
