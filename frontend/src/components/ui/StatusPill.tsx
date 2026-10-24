import clsx from 'clsx';
import { CheckCircle2, CircleSlash, FilePen, MessageSquareWarning, Send, UserSearch, type LucideIcon } from 'lucide-react';
import type { ProjectStatus } from '../../api/types';
import { STATUS_LABEL } from '../labels';

/** The single status-to-colour mapping in the app. Text steps are chosen for AA contrast on their tint. */
const STYLE: Record<ProjectStatus, { className: string; icon: LucideIcon }> = {
  DRAFT: { className: 'bg-amber-50 text-amber-800 ring-amber-200', icon: FilePen },
  SUBMITTED: { className: 'bg-sky-50 text-sky-800 ring-sky-200', icon: Send },
  UNDER_REVIEW: { className: 'bg-violet-50 text-violet-700 ring-violet-200', icon: UserSearch },
  REVISION_REQUESTED: { className: 'bg-rose-50 text-rose-700 ring-rose-200', icon: MessageSquareWarning },
  APPROVED: { className: 'bg-green-50 text-green-700 ring-green-200', icon: CheckCircle2 },
  REJECTED: { className: 'bg-slate-100 text-slate-700 ring-slate-300', icon: CircleSlash },
};

export function StatusPill({ status, size = 'md' }: { status: ProjectStatus; size?: 'sm' | 'md' }) {
  const { className, icon: Icon } = STYLE[status];
  return (
    <span
      className={clsx(
        'inline-flex items-center gap-1.5 whitespace-nowrap rounded-full font-semibold ring-1 ring-inset',
        size === 'sm' ? 'px-2.5 py-0.5 text-xs' : 'px-3 py-1 text-xs',
        className,
      )}
    >
      <Icon className="h-3.5 w-3.5" aria-hidden />
      {STATUS_LABEL[status]}
    </span>
  );
}
