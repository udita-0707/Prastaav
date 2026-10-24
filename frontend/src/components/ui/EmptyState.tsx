import type { ReactNode } from 'react';
import { illustrations } from '../../assets';

interface EmptyStateProps {
  title: string;
  description?: ReactNode;
  action?: ReactNode;
  image?: string;
}

export function EmptyState({ title, description, action, image = illustrations.emptyProjects }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center px-6 py-12 text-center">
      <img src={image} alt="" className="h-28 w-auto" />
      <h3 className="mt-5 text-base font-bold text-ink">{title}</h3>
      {description && <p className="mt-1.5 max-w-md text-sm text-muted">{description}</p>}
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}
