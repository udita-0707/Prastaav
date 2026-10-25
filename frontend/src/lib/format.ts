const DAY = 24 * 60 * 60 * 1000;

/** "12 Apr 2026" */
export function formatDay(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
}

/** "12 Apr 2026, 10:24" */
export function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}

export function formatTime(date: Date): string {
  return date.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
}

/** Whole days between the timestamp and now (0 for today). */
export function daysSince(iso: string, now = Date.now()): number {
  return Math.max(0, Math.floor((now - new Date(iso).getTime()) / DAY));
}

export function waitingLabel(iso: string): string {
  const days = daysSince(iso);
  if (days === 0) return 'Since today';
  return days === 1 ? '1 day waiting' : `${days} days waiting`;
}

export function greeting(hour = new Date().getHours()): string {
  if (hour < 12) return 'Good morning';
  if (hour < 17) return 'Good afternoon';
  return 'Good evening';
}
