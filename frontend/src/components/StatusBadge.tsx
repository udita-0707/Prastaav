import type { ProjectStatus } from '../api/types';
import { STATUS_LABEL } from './labels';

export function StatusBadge({ status }: { status: ProjectStatus }) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{STATUS_LABEL[status]}</span>;
}
