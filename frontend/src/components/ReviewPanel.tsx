import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { errorMessage } from '../api/client';
import { recordReview } from '../api/endpoints';
import type { Action, ReviewDecision } from '../api/types';
import { ErrorNote } from './ErrorNote';

const OPTIONS: { decision: ReviewDecision; label: string }[] = [
  { decision: 'APPROVE', label: 'Approve' },
  { decision: 'REQUEST_REVISION', label: 'Request revision' },
  { decision: 'REJECT', label: 'Reject' },
];

export function ReviewPanel({
  projectId,
  allowed,
  versionNumber,
}: {
  projectId: string;
  allowed: Action[];
  versionNumber: number | null;
}) {
  const queryClient = useQueryClient();
  const [decision, setDecision] = useState<ReviewDecision>('APPROVE');
  const [comments, setComments] = useState('');
  const needsComments = decision !== 'APPROVE';
  const mutation = useMutation({
    mutationFn: () => recordReview(projectId, { decision, comments, versionNumber }),
    onSuccess: () => {
      setComments('');
      queryClient.invalidateQueries({ queryKey: ['project', projectId] });
      queryClient.invalidateQueries({ queryKey: ['reviews', projectId] });
    },
  });

  return (
    <form
      className="stack"
      onSubmit={(e) => {
        e.preventDefault();
        mutation.mutate();
      }}
    >
      <p className="muted">You are reviewing version {versionNumber}.</p>
      <fieldset className="choices">
        <legend>Decision</legend>
        {OPTIONS.filter((o) => allowed.includes(o.decision)).map((o) => (
          <label key={o.decision} className={`choice choice-${o.decision.toLowerCase()}`}>
            <input
              type="radio"
              name="decision"
              checked={decision === o.decision}
              onChange={() => setDecision(o.decision)}
            />
            {o.label}
          </label>
        ))}
      </fieldset>
      <label className="field">
        <span>
          Comments {needsComments ? <em className="hint">required</em> : <em className="hint">optional</em>}
        </span>
        <textarea rows={4} value={comments} onChange={(e) => setComments(e.target.value)} />
      </label>
      <button className="primary" disabled={mutation.isPending || (needsComments && !comments.trim())}>
        Record decision
      </button>
      <ErrorNote message={mutation.error ? errorMessage(mutation.error) : null} />
    </form>
  );
}
