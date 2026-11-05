import clsx from 'clsx';
import { Check } from 'lucide-react';
import { Link } from 'react-router-dom';
import type { NotificationResponse } from '../api/types';
import { formatDateTime } from '../lib/format';
import { notificationLink, useMarkNotificationRead } from '../lib/useNotifications';

/** One inbox row, in the bell's dropdown and on the notifications page. Opening its project also marks it read. */
export function NotificationItem({ notification, onOpen }: { notification: NotificationResponse; onOpen?: () => void }) {
  const markRead = useMarkNotificationRead();
  const { id, message, read, createdAt } = notification;
  const link = notificationLink(notification);
  const text = <span className={clsx('block text-sm', read ? 'text-slate-700' : 'font-semibold text-ink')}>{message}</span>;

  return (
    <div className="flex items-start gap-3 px-4 py-3">
      <span className={clsx('mt-1.5 h-2 w-2 shrink-0 rounded-full', read ? 'bg-transparent' : 'bg-brand-600')} aria-hidden />
      <div className="min-w-0 flex-1">
        {!read && <span className="sr-only">Unread: </span>}
        {link ? (
          <Link
            to={link}
            className="rounded hover:text-brand-700 hover:underline"
            onClick={() => {
              if (!read) markRead.mutate(id);
              onOpen?.();
            }}
          >
            {text}
          </Link>
        ) : (
          text
        )}
        <time dateTime={createdAt} className="mt-0.5 block text-xs text-muted">
          {formatDateTime(createdAt)}
        </time>
      </div>
      {!read && (
        <button
          type="button"
          onClick={() => markRead.mutate(id)}
          disabled={markRead.isPending}
          aria-label={`Mark as read: ${message}`}
          title="Mark as read"
          className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-lg text-slate-500 hover:bg-slate-100 hover:text-ink disabled:opacity-40"
        >
          <Check className="h-4 w-4" aria-hidden />
        </button>
      )}
    </div>
  );
}
