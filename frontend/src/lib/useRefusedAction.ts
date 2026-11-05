import { useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { useCallback } from 'react';
import { useToast } from '../components/ui';

/** A project action answered 403 or 409: the page was out of date, because someone else moved the project on. */
export function isRefusedAction(error: unknown): boolean {
  if (!axios.isAxiosError(error)) return false;
  const status = error.response?.status;
  return status === 403 || status === 409;
}

/** After a refused action, reloads the project so its buttons match allowedActions again, and says why. */
export function useRefusedAction(projectId: string | undefined) {
  const queryClient = useQueryClient();
  const toast = useToast();
  return useCallback(
    (error: unknown) => {
      if (!isRefusedAction(error)) return;
      for (const key of ['project', 'versions', 'reviews', 'history']) queryClient.invalidateQueries({ queryKey: [key, projectId] });
      queryClient.invalidateQueries({ queryKey: ['projects'] });
      toast.show({ tone: 'error', title: 'That action is no longer available', description: 'This project changed since the page loaded. It now shows the current state.' });
    },
    [projectId, queryClient, toast],
  );
}
