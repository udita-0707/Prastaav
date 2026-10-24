import { createBrowserRouter, Navigate } from 'react-router-dom';
import { RequireAuth } from './auth/RequireAuth';
import { useAuth } from './auth/AuthContext';
import { AppShell } from './layouts/AppShell';
import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/LoginPage';
import { ProjectDetailPage } from './pages/ProjectDetailPage';
import { ProjectsPage } from './pages/ProjectsPage';
import { ProjectWizardPage } from './pages/ProjectWizardPage';
import { RegisterPage } from './pages/RegisterPage';

function RootRedirect() {
  const { user } = useAuth();
  return <Navigate to={user ? '/app' : '/login'} replace />;
}

export const router = createBrowserRouter([
  { path: '/', element: <RootRedirect /> },
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  {
    path: '/app',
    element: (
      <RequireAuth>
        <AppShell />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <HomePage /> },
      { path: 'projects', element: <ProjectsPage /> },
      // Distinct keys: saving a new project moves to the edit route, which must start with fresh state.
      { path: 'projects/new', element: <RequireAuth roles={['STUDENT']}><ProjectWizardPage key="new" /></RequireAuth> },
      { path: 'projects/:id', element: <ProjectDetailPage /> },
      { path: 'projects/:id/edit', element: <RequireAuth roles={['STUDENT']}><ProjectWizardPage key="edit" /></RequireAuth> },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
]);
