import { useAuth } from '../auth/AuthContext';
import { useDocumentTitle } from '../lib/useDocumentTitle';
import { CoordinatorHome } from './home/CoordinatorHome';
import { FacultyHome } from './home/FacultyHome';
import { StudentHome } from './home/StudentHome';

/** /app: each role gets its own home. */
export function HomePage() {
  useDocumentTitle('Home');
  const { user } = useAuth();
  if (!user) return null;
  switch (user.role) {
    case 'STUDENT':
      return <StudentHome user={user} />;
    case 'FACULTY':
      return <FacultyHome user={user} />;
    case 'COORDINATOR':
      return <CoordinatorHome user={user} />;
  }
}
