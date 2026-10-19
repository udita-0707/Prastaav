import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { errorMessage } from '../api/client';
import { assignReviewer, listFaculty } from '../api/endpoints';
import { ErrorNote } from './ErrorNote';

export function AssignReviewerPanel({ projectId }: { projectId: string }) {
  const queryClient = useQueryClient();
  const faculty = useQuery({ queryKey: ['faculty'], queryFn: listFaculty });
  const [facultyId, setFacultyId] = useState('');
  const mutation = useMutation({
    mutationFn: () => assignReviewer(projectId, facultyId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['project', projectId] }),
  });

  return (
    <form
      className="stack"
      onSubmit={(e) => {
        e.preventDefault();
        mutation.mutate();
      }}
    >
      <label className="field">
        <span>Reviewer</span>
        <select value={facultyId} onChange={(e) => setFacultyId(e.target.value)} required>
          <option value="">Choose a faculty member</option>
          {faculty.data?.map((f) => (
            <option key={f.id} value={f.id}>
              {f.name}
            </option>
          ))}
        </select>
      </label>
      <button className="primary" disabled={!facultyId || mutation.isPending}>
        Assign reviewer
      </button>
      <ErrorNote message={faculty.error ? errorMessage(faculty.error) : mutation.error ? errorMessage(mutation.error) : null} />
    </form>
  );
}
