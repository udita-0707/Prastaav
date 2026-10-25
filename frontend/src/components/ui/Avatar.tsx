import clsx from 'clsx';
import { avatarColor, initials } from '../../lib/people';

const SIZE = {
  xs: 'h-7 w-7 text-[11px]',
  sm: 'h-8 w-8 text-xs',
  md: 'h-10 w-10 text-sm',
  lg: 'h-12 w-12 text-base',
} as const;

type AvatarSize = keyof typeof SIZE;

export function Avatar({ name, size = 'md', className }: { name: string; size?: AvatarSize; className?: string }) {
  return (
    <span
      className={clsx('inline-flex shrink-0 select-none items-center justify-center rounded-full font-bold', SIZE[size], avatarColor(name), className)}
      title={name}
      aria-hidden
    >
      {initials(name)}
    </span>
  );
}

/** Overlapping avatars with a "+N" chip; the full list is in the accessible label. */
export function AvatarStack({ names, max = 3, size = 'sm' }: { names: string[]; max?: number; size?: AvatarSize }) {
  if (names.length === 0) return <span className="text-sm text-muted">No team listed</span>;
  const shown = names.slice(0, max);
  const extra = names.length - shown.length;
  return (
    <span className="inline-flex items-center" role="img" aria-label={`Team: ${names.join(', ')}`}>
      {shown.map((name, index) => (
        <Avatar key={`${name}-${index}`} name={name} size={size} className={clsx('ring-2 ring-white', index > 0 && '-ml-2')} />
      ))}
      {extra > 0 && (
        <span className={clsx('-ml-2 inline-flex items-center justify-center rounded-full bg-slate-100 font-semibold text-slate-600 ring-2 ring-white', SIZE[size])}>
          +{extra}
        </span>
      )}
    </span>
  );
}
