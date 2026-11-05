import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { UserPlus, UserRoundCog } from 'lucide-react';
import { useState } from 'react';
import { errorMessage } from '../api/client';
import { assignReviewer, listFaculty, reassignReviewer } from '../api/endpoints';
import type { UserSummary } from '../api/types';
import { ErrorNote } from './ErrorNote';
import { Button, Select, useToast } from './ui';

interface AssignReviewerPanelProps {
  projectId: string;
  projectTitle: string;
  /** The current reviewer of an UNDER_REVIEW project: the panel then hands the review to someone else. */
  reassignFrom?: UserSummary;
  onAssigned?: () => void;
}

/** Coordinator picks a faculty reviewer for a project. Used on the project page and in the assign dialog. */
export function AssignReviewerPanel({ projectId, projectTitle, reassignFrom, onAssigned }: AssignReviewerPanelProps) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const faculty = useQuery({ queryKey: ['faculty'], queryFn: listFaculty });
  const [facultyId, setFacultyId] = useState('');
  const options = faculty.data?.filter((f) => f.id !== reassignFrom?.id);
  const mutation = useMutation({
    mutationFn: () => (reassignFrom ? reassignReviewer(projectId, facultyId) : assignReviewer(projectId, facultyId)),
    onSuccess: (project) => {
      queryClient.invalidateQueries({ queryKey: ['project', projectId] });
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      toast.show({
        tone: 'success',
        title: reassignFrom ? 'Reviewer changed' : 'Reviewer assigned',
        description: `${project.reviewer?.name ?? 'The reviewer'} now reviews “${projectTitle}”.`,
      });
      setFacultyId('');
      onAssigned?.();
    },
  });
  const placeholder = faculty.isLoading ? 'Loading faculty…' : options?.length === 0 ? 'No other faculty member' : 'Choose a faculty member';

  return (
    <form
      className="space-y-4"
      onSubmit={(e) => {
        e.preventDefault();
        mutation.mutate();
      }}
    >
      {reassignFrom && <p className="text-sm text-slate-700">Hand this review to another faculty member. The project stays Under Review and both reviewers are notified.</p>}
      <Select label={reassignFrom ? 'New faculty reviewer' : 'Faculty reviewer'} value={facultyId} onChange={(e) => setFacultyId(e.target.value)} required disabled={faculty.isLoading}>
        <option value="">{placeholder}</option>
        {options?.map((f) => (
          <option key={f.id} value={f.id}>
            {f.name}
          </option>
        ))}
      </Select>
      <ErrorNote message={faculty.error ? errorMessage(faculty.error) : mutation.error ? errorMessage(mutation.error) : null} onRetry={faculty.error ? () => faculty.refetch() : undefined} />
      <Button
        type="submit"
        variant={reassignFrom ? 'outline' : 'primary'}
        className="w-full"
        disabled={!facultyId}
        loading={mutation.isPending}
        icon={reassignFrom ? <UserRoundCog className="h-4 w-4" aria-hidden /> : <UserPlus className="h-4 w-4" aria-hidden />}
      >
        {reassignFrom ? 'Reassign reviewer' : 'Assign reviewer'}
      </Button>
    </form>
  );
}
