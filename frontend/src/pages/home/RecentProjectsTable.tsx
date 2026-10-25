import { Eye, PencilLine } from 'lucide-react';
import { Link } from 'react-router-dom';
import type { ProjectSummaryResponse } from '../../api/types';
import { ButtonLink, DataTable, DropdownMenu, StatusPill, type Column } from '../../components/ui';
import { formatDay } from '../../lib/format';
import { ProjectIcon } from '../../lib/projectIcon';

/** Five latest projects on the student home. A draft offers "Edit draft"; everything else opens the project. */
export function RecentProjectsTable({ rows, loading }: { rows: ProjectSummaryResponse[] | undefined; loading: boolean }) {
  const columns: Column<ProjectSummaryResponse>[] = [
    {
      key: 'title',
      header: 'Title',
      cell: (p) => (
        <Link to={`/app/projects/${p.id}`} className="flex items-center gap-3 font-semibold text-ink hover:text-brand-700">
          <ProjectIcon id={p.id} size="sm" />
          <span className="line-clamp-1">{p.title}</span>
        </Link>
      ),
    },
    { key: 'status', header: 'Status', cell: (p) => <StatusPill status={p.status} /> },
    { key: 'updated', header: 'Last updated', cell: (p) => <span className="text-muted">{formatDay(p.updatedAt)}</span> },
    {
      key: 'actions',
      header: 'Actions',
      className: 'w-40 text-right',
      cell: (p) => (
        <div className="flex items-center justify-end gap-1">
          <ButtonLink to={`/app/projects/${p.id}`} variant="outline" size="sm">
            View
          </ButtonLink>
          <DropdownMenu
            label={`More actions for ${p.title}`}
            items={[
              { label: 'View project', icon: <Eye className="h-4 w-4" />, to: `/app/projects/${p.id}` },
              ...(p.status === 'DRAFT' ? [{ label: 'Edit draft', icon: <PencilLine className="h-4 w-4" />, to: `/app/projects/${p.id}/edit` }] : []),
            ]}
          />
        </div>
      ),
    },
  ];
  return <DataTable caption="Recent projects" columns={columns} rows={rows} rowKey={(p) => p.id} loading={loading} />;
}
