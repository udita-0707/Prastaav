import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { errorMessage } from '../api/client';
import { getUnreadNotificationCount, listNotifications, markAllNotificationsRead, markNotificationRead } from '../api/endpoints';
import type { NotificationResponse } from '../api/types';
import { useToast } from '../components/ui';

const POLL_INTERVAL_MS = 30_000;
const ROOT_KEY = 'notifications';

/** The bell's number, refreshed every 30 seconds. The user id keeps one account's inbox out of the next one's cache. */
export function useUnreadNotificationCount(userId: string) {
  return useQuery({ queryKey: [ROOT_KEY, userId, 'unread-count'], queryFn: getUnreadNotificationCount, refetchInterval: POLL_INTERVAL_MS });
}

/** One page of the inbox, newest first, refreshed like the count while it is on screen. */
export function useNotificationPage(userId: string, page: number, size: number, enabled = true) {
  return useQuery({
    queryKey: [ROOT_KEY, userId, 'list', { page, size }],
    queryFn: () => listNotifications(page, size),
    refetchInterval: POLL_INTERVAL_MS,
    placeholderData: keepPreviousData,
    enabled,
  });
}

export function useMarkNotificationRead() {
  const queryClient = useQueryClient();
  const toast = useToast();
  return useMutation({
    mutationFn: markNotificationRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [ROOT_KEY] }),
    onError: (error) => toast.show({ tone: 'error', title: 'Could not mark as read', description: errorMessage(error) }),
  });
}

export function useMarkAllNotificationsRead() {
  const queryClient = useQueryClient();
  const toast = useToast();
  return useMutation({
    mutationFn: markAllNotificationsRead,
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: [ROOT_KEY] });
      toast.show({ tone: 'success', title: updated === 1 ? '1 notification marked as read' : `${updated} notifications marked as read` });
    },
    onError: (error) => toast.show({ tone: 'error', title: 'Could not mark all as read', description: errorMessage(error) }),
  });
}

/** Where a notification leads. A reviewer who was moved off a project can no longer open it, so that one has no link. */
export function notificationLink({ projectId, type }: NotificationResponse): string | null {
  return projectId && type !== 'REVIEWER_UNASSIGNED' ? `/app/projects/${projectId}` : null;
}
