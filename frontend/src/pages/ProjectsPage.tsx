import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { ArrowRight, ArrowUpDown, CheckCircle2, Eye, FileText, FilePen, Filter, PencilLine, Plus, UserPlus, UserRoundSearch, UserSearch } from 'lucide-react';
import { useState, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { getProjectCounts, listFaculty, listProjects } from '../api/endpoints';
import type { ProjectSort, ProjectStatus, ProjectSummaryResponse, Role, UserResponse } from '../api/types';
import { illustrations } from '../assets';
import { useAuth } from '../auth/AuthContext';
import { ErrorNote } from '../components/ErrorNote';
import { STATUS_LABEL } from '../components/labels';
import { Button, ButtonLink, Card, CtaBand, DataTable, DropdownMenu, EmptyState, FilterBar, PageHeader, Pagination, SearchField, Select, StatCard, type Column } from '../components/ui';
import { useDocumentTitle } from '../lib/useDocumentTitle';
import { useProjectListParams } from '../lib/useProjectListParams';
import { AssignDialog, type AssignTarget } from './projects/AssignDialog';
import { projectColumns } from './projects/columns';

type Mode = 'projects' | 'reviews';

const HEADING: Record<Role, { title: string; description: string; empty: string }> = {
  STUDENT: { title: 'My Projects', description: 'Every proposal you have created, with its current status.', empty: 'You have no projects yet. Create one to start your proposal.' },
  FACULTY: { title: 'Assigned Projects', description: 'Projects the coordinator assigned to you for review.', empty: 'No projects are assigned to you yet. The coordinator assigns submitted proposals.' },
  COORDINATOR: { title: 'All Projects', description: 'View and manage all project proposals.', empty: 'No projects have been created yet.' },
};

const SORT_LABEL: Record<ProjectSort, string> = { LATEST: 'Latest', OLDEST: 'Oldest', TITLE: 'Title' };
const STATUSES = Object.keys(STATUS_LABEL) as ProjectStatus[];

function columnsFor(role: Role, mode: Mode, onAssign: (target: AssignTarget) => void): Column<ProjectSummaryResponse>[] {
  const actions: Column<ProjectSummaryResponse> = {
    key: 'actions',
    header: 'Actions',
    className: 'w-44 text-right',
    cell: (p) => {
      const canReview = role === 'FACULTY' && p.status === 'UNDER_REVIEW';
      const extra = [
        ...(role === 'STUDENT' && p.status === 'DRAFT' ? [{ label: 'Edit draft', icon: <PencilLine className="h-4 w-4" />, to: `/app/projects/${p.id}/edit` }] : []),
        ...(role === 'COORDINATOR' && p.status === 'SUBMITTED' ? [{ label: 'Assign faculty', icon: <UserPlus className="h-4 w-4" />, onSelect: () => onAssign(p) }] : []),
      ];
      return (
        <div className="flex items-center justify-end gap-1">
          {canReview ? (
            <ButtonLink to={`/app/projects/${p.id}#review`} size="sm" trailingIcon={<ArrowRight className="h-3.5 w-3.5" aria-hidden />}>
              Review
            </ButtonLink>
          ) : (
            <ButtonLink to={`/app/projects/${p.id}`} variant="outline" size="sm">
              View
            </ButtonLink>
          )}
          {extra.length > 0 ? (
            <DropdownMenu label={`More actions for ${p.title}`} items={[{ label: 'View project', icon: <Eye className="h-4 w-4" />, to: `/app/projects/${p.id}` }, ...extra]} />
          ) : (
            <span className="inline-block w-9" aria-hidden />
          )}
        </div>
      );
    },
  };
  const c = projectColumns;
  if (mode === 'reviews') return [c.title(), c.owner(), ...(role === 'COORDINATOR' ? [c.reviewer()] : []), c.status(), c.waiting(), actions];
  switch (role) {
    case 'STUDENT':
      return [c.title(), c.status(), c.reviewer('Reviewer'), c.updated(), actions];
    case 'FACULTY':
      return [c.title(), c.team(), c.owner(), c.status(), c.updated(), actions];
    case 'COORDINATOR':
      return [c.title(), c.team(true), c.status(), c.reviewer(), c.submitted(), actions];
  }
}

function CoordinatorStats() {
  const counts = useQuery({ queryKey: ['projects', 'summary'], queryFn: getProjectCounts });
  const by = counts.data?.byStatus;
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard label="Total projects" value={counts.data?.total} icon={<FileText className="h-6 w-6" />} tone="blue" loading={counts.isLoading} />
      <StatCard label="Draft" value={by?.DRAFT} icon={<FilePen className="h-6 w-6" />} tone="amber" loading={counts.isLoading} to="/app/projects?status=DRAFT" />
      <StatCard label="Under review" value={by?.UNDER_REVIEW} icon={<UserSearch className="h-6 w-6" />} tone="violet" loading={counts.isLoading} to="/app/projects?status=UNDER_REVIEW" />
      <StatCard label="Approved" value={by?.APPROVED} icon={<CheckCircle2 className="h-6 w-6" />} tone="green" loading={counts.isLoading} to="/app/projects?status=APPROVED" />
    </div>
  );
}

function AwaitingChip() {
  const counts = useQuery({ queryKey: ['projects', 'summary'], queryFn: getProjectCounts });
  const awaiting = counts.data?.awaitingAssignment ?? 0;
  if (awaiting === 0) return null;
  return (
    <Link to="/app/assign" className="inline-flex items-center gap-1.5 rounded-full bg-sky-50 px-3 py-1 text-xs font-semibold text-sky-800 ring-1 ring-inset ring-sky-200 hover:bg-sky-100">
      <UserPlus className="h-3.5 w-3.5" aria-hidden />
      {awaiting} awaiting assignment
    </Link>
  );
}

/**
 * One list page configured per role: My Projects (student), Assigned Projects (faculty), All Projects (coordinator).
 * In "reviews" mode it lists the role's Under Review projects, the longest-waiting first.
 */
export function ProjectsPage({ mode = 'projects' }: { mode?: Mode }) {
  const { user } = useAuth();
  return user ? <ProjectsList user={user} mode={mode} /> : null;
}

function ProjectsList({ user, mode }: { user: UserResponse; mode: Mode }) {
  const role = user.role;
  const isReviews = mode === 'reviews';
  const heading = isReviews
    ? {
        title: 'Reviews',
        description: role === 'FACULTY' ? 'Projects waiting for your decision, the longest-waiting first.' : 'Projects currently with a faculty reviewer, the longest-waiting first.',
        empty: role === 'FACULTY' ? 'Nothing is waiting for your review.' : 'No project is under review right now.',
      }
    : HEADING[role];
  useDocumentTitle(heading.title);

  const { params, search, setSearch, setStatus, setReviewer, setSort, setPage } = useProjectListParams(isReviews ? { status: ['UNDER_REVIEW'], sort: 'OLDEST' } : {});
  const projects = useQuery({ queryKey: ['projects', 'list', params], queryFn: () => listProjects(params), placeholderData: keepPreviousData });
  const faculty = useQuery({ queryKey: ['faculty'], queryFn: listFaculty, enabled: role === 'COORDINATOR' });
  const [assignTarget, setAssignTarget] = useState<AssignTarget | null>(null);

  const statusValue = params.status?.join(',') ?? '';
  const filtered = Boolean(params.q || params.reviewerId || (!isReviews && statusValue));
  const columns = columnsFor(role, mode, setAssignTarget);

  let actions: ReactNode = null;
  if (role === 'STUDENT' && !isReviews) {
    actions = (
      <ButtonLink to="/app/projects/new" icon={<Plus className="h-4 w-4" aria-hidden />}>
        New Project
      </ButtonLink>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title={heading.title}
        description={heading.description}
        badge={role === 'COORDINATOR' && !isReviews ? <AwaitingChip /> : undefined}
        actions={actions}
        illustration={role === 'COORDINATOR' && !isReviews ? illustrations.coordinatorLaptop : undefined}
      />

      {role === 'COORDINATOR' && !isReviews && <CoordinatorStats />}

      <Card className="space-y-5 p-5 sm:p-6">
        <FilterBar>
          <SearchField value={search} onChange={setSearch} placeholder={role === 'COORDINATOR' ? 'Search by title, team or faculty…' : 'Search by title, description or team…'} label="Search projects" />
          {!isReviews && (
            <Select aria-label="Filter by status" icon={<Filter className="h-4 w-4" />} value={statusValue} onChange={(e) => setStatus(e.target.value)} wrapperClassName="md:w-52">
              <option value="">All statuses</option>
              {statusValue.includes(',') && <option value={statusValue}>{params.status!.map((s) => STATUS_LABEL[s]).join(' + ')}</option>}
              {STATUSES.map((s) => (
                <option key={s} value={s}>
                  {STATUS_LABEL[s]}
                </option>
              ))}
            </Select>
          )}
          {role === 'COORDINATOR' && (
            <Select aria-label="Filter by faculty" icon={<UserRoundSearch className="h-4 w-4" />} value={params.reviewerId ?? ''} onChange={(e) => setReviewer(e.target.value)} wrapperClassName="md:w-52">
              <option value="">All faculty</option>
              {faculty.data?.map((f) => (
                <option key={f.id} value={f.id}>
                  {f.name}
                </option>
              ))}
            </Select>
          )}
          <Select aria-label="Sort projects" icon={<ArrowUpDown className="h-4 w-4" />} value={params.sort} onChange={(e) => setSort(e.target.value as ProjectSort)} wrapperClassName="md:w-44">
            {(Object.keys(SORT_LABEL) as ProjectSort[]).map((s) => (
              <option key={s} value={s}>
                Sort: {SORT_LABEL[s]}
              </option>
            ))}
          </Select>
        </FilterBar>

        <ErrorNote message={projects.error ? errorMessage(projects.error) : null} onRetry={() => projects.refetch()} />

        <DataTable
          caption={heading.title}
          columns={columns}
          rows={projects.data?.items}
          rowKey={(p) => p.id}
          loading={projects.isLoading}
          empty={
            filtered ? (
              <EmptyState
                title="No projects match"
                description="Try a different search or clear the filters."
                action={
                  <Button
                    variant="outline"
                    onClick={() => {
                      setSearch('');
                      setStatus('');
                      setReviewer('');
                    }}
                  >
                    Clear filters
                  </Button>
                }
              />
            ) : (
              <EmptyState
                title={isReviews ? 'All caught up' : 'No projects yet'}
                description={heading.empty}
                action={
                  role === 'STUDENT' && (
                    <ButtonLink to="/app/projects/new" icon={<Plus className="h-4 w-4" aria-hidden />}>
                      Create project
                    </ButtonLink>
                  )
                }
              />
            )
          }
        />

        {projects.data && <Pagination page={params.page ?? 0} size={params.size ?? 10} total={projects.data.total} onChange={setPage} />}
      </Card>

      {role === 'COORDINATOR' && !isReviews && (
        <CtaBand
          image={illustrations.assignFaculty}
          title="Need to assign faculty?"
          description="Submitted projects wait for a reviewer. Assign one and the project moves to Under Review."
          action={
            <ButtonLink to="/app/assign" icon={<UserPlus className="h-4 w-4" aria-hidden />} trailingIcon={<ArrowRight className="h-4 w-4" aria-hidden />}>
              Assign faculty
            </ButtonLink>
          }
        />
      )}

      <AssignDialog target={assignTarget} onClose={() => setAssignTarget(null)} />
    </div>
  );
}
