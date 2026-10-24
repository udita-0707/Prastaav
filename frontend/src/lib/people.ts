const AVATAR_COLORS = [
  'bg-brand-600 text-white',
  'bg-accent-600 text-white',
  'bg-violet-600 text-white',
  'bg-amber-600 text-white',
  'bg-rose-600 text-white',
  'bg-sky-700 text-white',
  'bg-green-700 text-white',
  'bg-indigo-600 text-white',
] as const;

/** Up to two initials, skipping honorifics such as "Dr." and "Prof.". */
export function initials(name: string): string {
  const words = name
    .replace(/\b(dr|prof|mr|mrs|ms)\.?\s+/gi, '')
    .trim()
    .split(/\s+/)
    .filter(Boolean);
  if (words.length === 0) return '?';
  const first = words[0][0];
  const last = words.length > 1 ? words[words.length - 1][0] : '';
  return (first + last).toUpperCase();
}

/** The same name always gets the same colour. */
export function avatarColor(name: string): string {
  let hash = 0;
  for (const char of name.toLowerCase()) hash = (hash * 31 + char.charCodeAt(0)) >>> 0;
  return AVATAR_COLORS[hash % AVATAR_COLORS.length];
}

/** Team members are stored as free text; the UI treats commas, semicolons and new lines as separators. */
export function parseTeam(text: string | null | undefined): string[] {
  if (!text) return [];
  return text
    .split(/[,;\n]/)
    .map((name) => name.trim())
    .filter(Boolean);
}

export function firstName(name: string): string {
  const words = name.replace(/\b(dr|prof|mr|mrs|ms)\.?\s+/gi, '').trim().split(/\s+/);
  return words[0] ?? name;
}
