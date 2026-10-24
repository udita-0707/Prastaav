import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { UserPlus } from 'lucide-react';
import { useState } from 'react';
import { errorMessage } from '../api/client';
import { assignReviewer, listFaculty } from '../api/endpoints';
import { ErrorNote } from './ErrorNote';
import { Button, Select, useToast } from './ui';

/** Coordinator picks a faculty reviewer for a SUBMITTED project. Used on the project page and in the assign dialog. */
export function AssignReviewerPanel({ projectId, projectTitle, onAssigned }: { projectId: string; projectTitle: string; onAssigned?: () => void }) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const faculty = useQuery({ queryKey: ['faculty'], queryFn: listFaculty });
  const [facultyId, setFacultyId] = useState('');
  const mutation = useMutation({
    mutationFn: () => assignReviewer(projectId, facultyId),
    onSuccess: (project) => {
      queryClient.invalidateQueries({ queryKey: ['project', projectId] });
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      toast.show({ tone: 'success', title: 'Reviewer assigned', description: `${project.reviewer?.name ?? 'The reviewer'} now reviews “${projectTitle}”.` });
      onAssigned?.();
    },
  });

  return (
    <form
      className="space-y-4"
      onSubmit={(e) => {
        e.preventDefault();
        mutation.mutate();
      }}
    >
      <Select label="Faculty reviewer" value={facultyId} onChange={(e) => setFacultyId(e.target.value)} required disabled={faculty.isLoading}>
        <option value="">{faculty.isLoading ? 'Loading faculty…' : 'Choose a faculty member'}</option>
        {faculty.data?.map((f) => (
          <option key={f.id} value={f.id}>
            {f.name}
          </option>
        ))}
      </Select>
      <ErrorNote message={faculty.error ? errorMessage(faculty.error) : mutation.error ? errorMessage(mutation.error) : null} onRetry={faculty.error ? () => faculty.refetch() : undefined} />
      <Button type="submit" className="w-full" disabled={!facultyId} loading={mutation.isPending} icon={<UserPlus className="h-4 w-4" aria-hidden />}>
        Assign reviewer
      </Button>
    </form>
  );
}
