import { useAuth } from '../auth/AuthContext';
import { useDocumentTitle } from '../lib/useDocumentTitle';
import { StudentHome } from './home/StudentHome';
import { ProjectsPage } from './ProjectsPage';

/** /app: each role gets its own home. */
export function HomePage() {
  useDocumentTitle('Home');
  const { user } = useAuth();
  if (!user) return null;
  switch (user.role) {
    case 'STUDENT':
      return <StudentHome user={user} />;
    default:
      return <ProjectsPage />;
  }
}
