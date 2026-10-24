import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { getProject, updateDraft } from '../api/endpoints';
import type { ProposalContent } from '../api/types';
import { ErrorNote } from '../components/ErrorNote';
import { EMPTY_PROPOSAL, ProposalFields } from '../components/ProposalFields';

export function ProposalEditorPage() {
  const { id = '' } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const project = useQuery({ queryKey: ['project', id], queryFn: () => getProject(id) });
  const [content, setContent] = useState<ProposalContent>(EMPTY_PROPOSAL);
  const draft = project.data?.currentProposal;

  useEffect(() => {
    if (draft) {
      setContent({
        problemStatement: draft.problemStatement ?? '',
        objectives: draft.objectives ?? '',
        methodology: draft.methodology ?? '',
        expectedOutcome: draft.expectedOutcome ?? '',
        timeline: draft.timeline ?? '',
        referenceLink: draft.referenceLink ?? '',
      });
    }
  }, [draft]);

  const mutation = useMutation({
    mutationFn: () => updateDraft(id, { ...content, referenceLink: content.referenceLink?.trim() || null }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['project', id] });
      navigate(`/app/projects/${id}`);
    },
  });

  if (project.isLoading) return <p className="muted">Loading…</p>;
  if (project.error) return <ErrorNote message={errorMessage(project.error)} />;
  if (!project.data?.allowedActions.includes('EDIT_DRAFT') || !draft) {
    return (
      <section className="narrow">
        <h1>Nothing to edit</h1>
        <p>This project has no open draft. Submitted versions cannot be changed.</p>
        <Link to={`/app/projects/${id}`}>Back to the project</Link>
      </section>
    );
  }

  return (
    <section className="narrow">
      <Link to={`/app/projects/${id}`} className="muted">
        {project.data.title}
      </Link>
      <h1>Edit draft, version {draft.versionNumber}</h1>
      <form
        className="stack"
        onSubmit={(e) => {
          e.preventDefault();
          mutation.mutate();
        }}
      >
        <ProposalFields value={content} onChange={setContent} />
        <div className="row">
          <button className="primary" disabled={mutation.isPending}>
            Save draft
          </button>
          <Link to={`/app/projects/${id}`} className="button">
            Cancel
          </Link>
        </div>
        <ErrorNote message={mutation.error ? errorMessage(mutation.error) : null} />
      </form>
    </section>
  );
}
