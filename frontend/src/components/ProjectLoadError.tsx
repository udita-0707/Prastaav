import axios from 'axios';
import { ArrowLeft } from 'lucide-react';
import { errorMessage } from '../api/client';
import { ErrorNote } from './ErrorNote';
import { ButtonLink, Card, EmptyState } from './ui';

/** A project page that could not load. Trying again cannot help with a missing or forbidden project, so those offer a way back. */
export function ProjectLoadError({ error, onRetry }: { error: unknown; onRetry: () => void }) {
  const status = axios.isAxiosError(error) ? error.response?.status : undefined;
  if (status === 400 || status === 403 || status === 404) {
    return (
      <Card>
        <EmptyState
          title="This project is not available"
          description={status === 403 ? 'You are not a participant of this project. A reviewer who has been replaced no longer has access.' : 'It does not exist, or the link is not correct.'}
          action={
            <ButtonLink to="/app/projects" variant="outline" icon={<ArrowLeft className="h-4 w-4" aria-hidden />}>
              Back to projects
            </ButtonLink>
          }
        />
      </Card>
    );
  }
  return <ErrorNote message={errorMessage(error)} onRetry={onRetry} />;
}
