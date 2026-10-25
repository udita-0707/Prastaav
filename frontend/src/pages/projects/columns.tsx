import { Link } from 'react-router-dom';
import type { ProjectSummaryResponse } from '../../api/types';
import { Avatar, AvatarStack, StatusPill, type Column } from '../../components/ui';
import { formatDateTime, formatDay, waitingLabel } from '../../lib/format';
import { parseTeam } from '../../lib/people';
import { ProjectIcon } from '../../lib/projectIcon';

type Row = ProjectSummaryResponse;

/** Column building blocks shared by every project table, so the lists look and read the same everywhere. */
export const projectColumns = {
  title: (): Column<Row> => ({
    key: 'title',
    header: 'Title',
    className: 'min-w-[16rem]',
    cell: (p) => (
      <Link to={`/app/projects/${p.id}`} className="group flex items-center gap-3">
        <ProjectIcon id={p.id} />
        <span className="min-w-0">
          <span className="block font-semibold text-ink group-hover:text-brand-700">{p.title}</span>
          <span className="mt-0.5 line-clamp-1 block text-xs text-muted">{p.description}</span>
        </span>
      </Link>
    ),
  }),
  team: (withCount = false): Column<Row> => ({
    key: 'team',
    header: withCount ? 'Team' : 'Team members',
    cell: (p) => {
      const team = parseTeam(p.teamMembers);
      return (
        <span className="flex flex-col gap-1">
          <AvatarStack names={team} />
          {withCount && team.length > 0 && <span className="text-xs text-muted">{team.length === 1 ? '1 member' : `${team.length} members`}</span>}
        </span>
      );
    },
  }),
  status: (): Column<Row> => ({ key: 'status', header: 'Status', cell: (p) => <StatusPill status={p.status} /> }),
  owner: (header = 'Student'): Column<Row> => ({
    key: 'owner',
    header,
    cell: (p) => (
      <span className="flex items-center gap-2.5">
        <Avatar name={p.owner.name} size="sm" />
        <span className="whitespace-nowrap font-medium text-ink">{p.owner.name}</span>
      </span>
    ),
  }),
  reviewer: (header = 'Faculty'): Column<Row> => ({
    key: 'reviewer',
    header,
    cell: (p) =>
      p.reviewer ? (
        <span className="flex items-center gap-2.5">
          <Avatar name={p.reviewer.name} size="sm" />
          <span className="whitespace-nowrap font-medium text-ink">{p.reviewer.name}</span>
        </span>
      ) : (
        <span className="text-muted">Unassigned</span>
      ),
  }),
  updated: (): Column<Row> => ({ key: 'updated', header: 'Last updated', cell: (p) => <span className="whitespace-nowrap text-muted">{formatDay(p.updatedAt)}</span> }),
  submitted: (): Column<Row> => ({
    key: 'submitted',
    header: 'Submitted on',
    cell: (p) => (p.submittedAt ? <span className="whitespace-nowrap text-muted">{formatDateTime(p.submittedAt)}</span> : <span className="text-muted">Not yet</span>),
  }),
  waiting: (): Column<Row> => ({ key: 'waiting', header: 'Waiting', cell: (p) => <span className="whitespace-nowrap text-muted">{waitingLabel(p.updatedAt)}</span> }),
};
