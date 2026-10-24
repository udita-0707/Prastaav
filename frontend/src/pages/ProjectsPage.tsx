import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { listProjects } from '../api/endpoints';
import type { Role } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { ErrorNote } from '../components/ErrorNote';
import { formatDate } from '../components/labels';
import { StatusBadge } from '../components/StatusBadge';

const HEADING: Record<Role, string> = {
  STUDENT: 'My projects',
  FACULTY: 'Projects assigned to me',
  COORDINATOR: 'All projects',
};

const EMPTY: Record<Role, string> = {
  STUDENT: 'You have no projects yet. Create one to start your proposal.',
  FACULTY: 'No projects are assigned to you yet. The coordinator assigns submitted proposals.',
  COORDINATOR: 'No projects have been created yet.',
};

export function ProjectsPage() {
  const { user } = useAuth();
  const projects = useQuery({ queryKey: ['projects'], queryFn: () => listProjects().then((page) => page.items) });
  if (!user) return null;

  return (
    <section>
      <div className="page-head">
        <h1>{HEADING[user.role]}</h1>
        {user.role === 'STUDENT' && (
          <Link to="/app/projects/new" className="button primary">
            New project
          </Link>
        )}
      </div>
      <ErrorNote message={projects.error ? errorMessage(projects.error) : null} />
      {projects.isLoading && <p className="muted">Loading projects…</p>}
      {projects.data && projects.data.length === 0 && <p className="empty">{EMPTY[user.role]}</p>}
      {projects.data && projects.data.length > 0 && (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Project</th>
                <th>Status</th>
                {user.role !== 'STUDENT' && <th>Student</th>}
                <th>Reviewer</th>
                <th>Last change</th>
              </tr>
            </thead>
            <tbody>
              {projects.data.map((p) => (
                <tr key={p.id}>
                  <td>
                    <Link to={`/app/projects/${p.id}`}>{p.title}</Link>
                  </td>
                  <td>
                    <StatusBadge status={p.status} />
                  </td>
                  {user.role !== 'STUDENT' && <td>{p.owner.name}</td>}
                  <td>{p.reviewer?.name ?? <span className="muted">Not assigned</span>}</td>
                  <td className="muted">{formatDate(p.updatedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
