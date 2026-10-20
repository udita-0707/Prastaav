import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { createProject } from '../api/endpoints';
import { ErrorNote } from '../components/ErrorNote';
import { EMPTY_PROPOSAL, ProposalFields } from '../components/ProposalFields';

export function CreateProjectPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [techStack, setTechStack] = useState('');
  const [teamMembers, setTeamMembers] = useState('');
  const [proposal, setProposal] = useState(EMPTY_PROPOSAL);
  const mutation = useMutation({
    mutationFn: () => createProject({ title, description, techStack, teamMembers, proposal }),
    onSuccess: (project) => {
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      navigate(`/projects/${project.id}`);
    },
  });

  return (
    <section className="narrow">
      <h1>New project</h1>
      <p className="muted">
        This creates the project and draft version 1. You can save an incomplete draft and finish it later.
      </p>
      <form
        className="stack"
        onSubmit={(e) => {
          e.preventDefault();
          mutation.mutate();
        }}
      >
        <label className="field">
          <span>Title</span>
          <input value={title} onChange={(e) => setTitle(e.target.value)} required maxLength={200} />
        </label>
        <label className="field">
          <span>Short description</span>
          <textarea rows={3} value={description} onChange={(e) => setDescription(e.target.value)} required />
        </label>
        <div className="two-col">
          <label className="field">
            <span>Tech stack</span>
            <input value={techStack} onChange={(e) => setTechStack(e.target.value)} placeholder="e.g. Java, React" />
          </label>
          <label className="field">
            <span>Team members</span>
            <input value={teamMembers} onChange={(e) => setTeamMembers(e.target.value)} placeholder="Names, comma separated" />
          </label>
        </div>
        <h2>Proposal, version 1</h2>
        <ProposalFields value={proposal} onChange={setProposal} />
        <button className="primary" disabled={mutation.isPending}>
          Create project
        </button>
        <ErrorNote message={mutation.error ? errorMessage(mutation.error) : null} />
      </form>
    </section>
  );
}
