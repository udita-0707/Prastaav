import { CheckCheck } from 'lucide-react';
import { useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { ErrorNote } from '../components/ErrorNote';
import { NotificationItem } from '../components/NotificationItem';
import { Button, Card, EmptyState, PageHeader, Pagination, Skeleton } from '../components/ui';
import { useDocumentTitle } from '../lib/useDocumentTitle';
import { useMarkAllNotificationsRead, useNotificationPage, useUnreadNotificationCount } from '../lib/useNotifications';

const PAGE_SIZE = 10;

/** The signed-in user's whole inbox, newest first. The page number lives in the URL, like the project lists. */
export function NotificationsPage() {
  useDocumentTitle('Notifications');
  const { user } = useAuth();
  const userId = user?.id ?? '';
  const [searchParams, setSearchParams] = useSearchParams();
  const page = Math.max(0, Number(searchParams.get('page') ?? 0) || 0);
  const notifications = useNotificationPage(userId, page, PAGE_SIZE);
  const unread = useUnreadNotificationCount(userId).data ?? 0;
  const markAll = useMarkAllNotificationsRead();

  const setPage = (next: number) => setSearchParams(next > 0 ? { page: String(next) } : {}, { replace: true });

  // An old link can point past the end of the inbox: show the last page instead of an empty one.
  const totalPages = notifications.data?.totalPages ?? 0;
  useEffect(() => {
    if (totalPages > 0 && page >= totalPages) setPage(totalPages - 1);
  }, [page, totalPages]);

  return (
    <div className="space-y-6">
      <PageHeader
        title="Notifications"
        description={unread > 0 ? `You have ${unread} unread.` : 'You are all caught up.'}
        actions={
          <Button variant="outline" onClick={() => markAll.mutate()} disabled={unread === 0} loading={markAll.isPending} icon={<CheckCheck className="h-4 w-4" aria-hidden />}>
            Mark all read
          </Button>
        }
      />
      <ErrorNote message={notifications.error ? errorMessage(notifications.error) : null} onRetry={() => notifications.refetch()} />
      <Card className="overflow-hidden">
        {notifications.isLoading && (
          <div className="space-y-3 p-5">
            {[0, 1, 2, 3].map((i) => (
              <Skeleton key={i} className="h-14 w-full" />
            ))}
          </div>
        )}
        {notifications.data?.totalElements === 0 && <EmptyState title="No notifications yet" description="Assignments, review decisions and resubmissions on your projects will appear here." />}
        {notifications.data && notifications.data.content.length > 0 && (
          <ul className="divide-y divide-line/80">
            {notifications.data.content.map((notification) => (
              <li key={notification.id}>
                <NotificationItem notification={notification} />
              </li>
            ))}
          </ul>
        )}
      </Card>
      {notifications.data && <Pagination page={notifications.data.page} size={PAGE_SIZE} total={notifications.data.totalElements} onChange={setPage} />}
    </div>
  );
}
