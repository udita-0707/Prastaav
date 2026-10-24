import { createBrowserRouter, Navigate } from 'react-router-dom';
import { RequireAuth } from './auth/RequireAuth';
import { useAuth } from './auth/AuthContext';
import { AppShell } from './layouts/AppShell';
import { CreateProjectPage } from './pages/CreateProjectPage';
import { LoginPage } from './pages/LoginPage';
import { ProjectDetailPage } from './pages/ProjectDetailPage';
import { ProjectsPage } from './pages/ProjectsPage';
import { ProposalEditorPage } from './pages/ProposalEditorPage';
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
      { index: true, element: <ProjectsPage /> },
      { path: 'projects', element: <ProjectsPage /> },
      { path: 'projects/new', element: <RequireAuth roles={['STUDENT']}><CreateProjectPage /></RequireAuth> },
      { path: 'projects/:id', element: <ProjectDetailPage /> },
      { path: 'projects/:id/edit', element: <RequireAuth roles={['STUDENT']}><ProposalEditorPage /></RequireAuth> },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
]);
