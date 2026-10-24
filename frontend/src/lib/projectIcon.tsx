import { BookOpen, Bot, Brain, Cpu, GraduationCap, HeartPulse, Leaf, Lightbulb, Rocket, Sprout, type LucideIcon } from 'lucide-react';
import { IconTile, type Tone } from '../components/ui';

const ICONS: LucideIcon[] = [GraduationCap, Leaf, Cpu, Lightbulb, Bot, Brain, Rocket, BookOpen, HeartPulse, Sprout];
const TONES: Tone[] = ['blue', 'teal', 'violet', 'amber', 'sky', 'green', 'rose'];

function hash(text: string): number {
  let value = 0;
  for (const char of text) value = (value * 33 + char.charCodeAt(0)) >>> 0;
  return value;
}

/** Decorative tile for a project row. Chosen from the id, so a project keeps its icon everywhere. */
export function ProjectIcon({ id, size = 'md' }: { id: string; size?: 'sm' | 'md' }) {
  const h = hash(id);
  const Icon = ICONS[h % ICONS.length];
  return (
    <IconTile tone={TONES[h % TONES.length]} size={size}>
      <Icon className="h-5 w-5" />
    </IconTile>
  );
}
