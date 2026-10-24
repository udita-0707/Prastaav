import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { UserPlus } from 'lucide-react';
import { useState } from 'react';
import { errorMessage } from '../api/client';
import { listProjects } from '../api/endpoints';
import type { ProjectSummaryResponse } from '../api/types';
import { illustrations } from '../assets';
import { ErrorNote } from '../components/ErrorNote';
import { Button, Card, DataTable, EmptyState, FilterBar, PageHeader, Pagination, SearchField, type Column } from '../components/ui';
import { useDocumentTitle } from '../lib/useDocumentTitle';
import { useProjectListParams } from '../lib/useProjectListParams';
import { AssignDialog, type AssignTarget } from './projects/AssignDialog';
import { projectColumns } from './projects/columns';

/** Coordinator: submitted projects that still need a reviewer, the longest-waiting first. */
export function AssignPage() {
  useDocumentTitle('Assign faculty');
  const { params, search, setSearch, setPage } = useProjectListParams({ status: ['SUBMITTED'], sort: 'OLDEST' });
  const query = { ...params, status: ['SUBMITTED' as const] };
  const projects = useQuery({ queryKey: ['projects', 'list', query], queryFn: () => listProjects(query), placeholderData: keepPreviousData });
  const [target, setTarget] = useState<AssignTarget | null>(null);

  const columns: Column<ProjectSummaryResponse>[] = [
    projectColumns.title(),
    projectColumns.owner(),
    projectColumns.team(true),
    projectColumns.submitted(),
    projectColumns.waiting(),
    {
      key: 'assign',
      header: 'Action',
      className: 'text-right',
      cell: (p) => (
        <Button size="sm" onClick={() => setTarget(p)} icon={<UserPlus className="h-4 w-4" aria-hidden />}>
          Assign faculty
        </Button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <PageHeader
        title="Assign Faculty"
        description="Submitted projects waiting for a reviewer. Assigning one moves the project to Under Review."
        illustration={illustrations.assignFaculty}
      />
      <Card className="space-y-5 p-5 sm:p-6">
        <FilterBar>
          <SearchField value={search} onChange={setSearch} placeholder="Search by title, student or team…" label="Search submitted projects" />
        </FilterBar>
        <ErrorNote message={projects.error ? errorMessage(projects.error) : null} onRetry={() => projects.refetch()} />
        <DataTable
          caption="Submitted projects waiting for a reviewer"
          columns={columns}
          rows={projects.data?.items}
          rowKey={(p) => p.id}
          loading={projects.isLoading}
          empty={
            <EmptyState
              image={illustrations.assignFaculty}
              title={params.q ? 'No submitted project matches' : 'Nothing to assign'}
              description={params.q ? 'Try a different search.' : 'Every submitted project already has a reviewer.'}
            />
          }
        />
        {projects.data && <Pagination page={params.page ?? 0} size={params.size ?? 10} total={projects.data.total} onChange={setPage} />}
      </Card>
      <AssignDialog target={target} onClose={() => setTarget(null)} />
    </div>
  );
}
