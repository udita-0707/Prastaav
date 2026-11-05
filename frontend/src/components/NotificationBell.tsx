import { Bell } from 'lucide-react';
import { useEffect, useId, useRef, useState, type KeyboardEvent } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { useNotificationPage, useUnreadNotificationCount } from '../lib/useNotifications';
import { ErrorNote } from './ErrorNote';
import { NotificationItem } from './NotificationItem';
import { Skeleton } from './ui';

const LATEST_COUNT = 5;
const MAX_BADGE = 99;

/** Top-bar bell: the unread count, and a dropdown with the latest notifications. Escape closes it and returns focus. */
export function NotificationBell({ userId }: { userId: string }) {
  const [open, setOpen] = useState(false);
  const panelId = useId();
  const root = useRef<HTMLDivElement>(null);
  const button = useRef<HTMLButtonElement>(null);
  const { pathname } = useLocation();
  const unread = useUnreadNotificationCount(userId).data ?? 0;
  const latest = useNotificationPage(userId, 0, LATEST_COUNT, open);

  useEffect(() => setOpen(false), [pathname]);
  useEffect(() => {
    if (!open) return;
    const onPointer = (event: PointerEvent) => {
      if (!root.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener('pointerdown', onPointer);
    return () => document.removeEventListener('pointerdown', onPointer);
  }, [open]);

  const onKey = (event: KeyboardEvent) => {
    if (event.key === 'Escape' && open) {
      setOpen(false);
      button.current?.focus();
    }
  };

  return (
    <div ref={root} className="relative" onKeyDown={onKey}>
      <button
        ref={button}
        type="button"
        onClick={() => setOpen((o) => !o)}
        aria-expanded={open}
        aria-controls={open ? panelId : undefined}
        aria-label={unread > 0 ? `Notifications, ${unread} unread` : 'Notifications'}
        className="relative inline-flex h-10 w-10 items-center justify-center rounded-xl text-slate-600 hover:bg-slate-100 hover:text-ink"
      >
        <Bell className="h-5 w-5" aria-hidden />
        {unread > 0 && (
          <span className="absolute -right-0.5 -top-0.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-rose-600 px-1 text-[11px] font-bold tabular-nums text-white" aria-hidden>
            {unread > MAX_BADGE ? `${MAX_BADGE}+` : unread}
          </span>
        )}
      </button>
      {open && (
        <div
          id={panelId}
          role="region"
          aria-label="Latest notifications"
          className="fixed inset-x-4 top-[72px] z-30 overflow-hidden rounded-xl border border-line bg-white shadow-pop sm:absolute sm:inset-x-auto sm:right-0 sm:top-full sm:mt-2 sm:w-96"
        >
          <div className="flex items-center justify-between border-b border-line/80 px-4 py-3">
            <h2 className="text-sm font-bold text-ink">Notifications</h2>
            {unread > 0 && <span className="text-xs font-medium text-muted">{unread} unread</span>}
          </div>
          <div className="max-h-[60vh] overflow-y-auto">
            {latest.isLoading && (
              <div className="space-y-3 p-4">
                {[0, 1, 2].map((i) => (
                  <Skeleton key={i} className="h-12 w-full" />
                ))}
              </div>
            )}
            {latest.error && (
              <div className="p-3">
                <ErrorNote message={errorMessage(latest.error)} onRetry={() => latest.refetch()} />
              </div>
            )}
            {latest.data?.content.length === 0 && <p className="px-4 py-8 text-center text-sm text-muted">You have no notifications yet.</p>}
            {latest.data && latest.data.content.length > 0 && (
              <ul className="divide-y divide-line/80">
                {latest.data.content.map((notification) => (
                  <li key={notification.id}>
                    <NotificationItem notification={notification} onOpen={() => setOpen(false)} />
                  </li>
                ))}
              </ul>
            )}
          </div>
          <Link to="/app/notifications" className="block border-t border-line/80 px-4 py-3 text-center text-sm font-semibold text-accent-700 hover:bg-slate-50">
            View all notifications
          </Link>
        </div>
      )}
    </div>
  );
}
