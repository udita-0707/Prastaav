import { createBrowserRouter, Navigate } from 'react-router-dom';
import { RequireAuth } from './auth/RequireAuth';
import { AppShell } from './layouts/AppShell';
import { AssignPage } from './pages/AssignPage';
import { HomePage } from './pages/HomePage';
import { LandingPage } from './pages/LandingPage';
import { LoginPage } from './pages/LoginPage';
import { ProjectDetailPage } from './pages/ProjectDetailPage';
import { ProjectsPage } from './pages/ProjectsPage';
import { ProjectWizardPage } from './pages/ProjectWizardPage';
import { RegisterPage } from './pages/RegisterPage';

export const router = createBrowserRouter([
  { path: '/', element: <LandingPage /> },
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
      { path: 'projects', element: <ProjectsPage key="projects" /> },
      // Distinct keys: saving a new project moves to the edit route, which must start with fresh state.
      { path: 'projects/new', element: <RequireAuth roles={['STUDENT']}><ProjectWizardPage key="new" /></RequireAuth> },
      { path: 'projects/:id', element: <ProjectDetailPage /> },
      { path: 'reviews', element: <RequireAuth roles={['FACULTY', 'COORDINATOR']}><ProjectsPage key="reviews" mode="reviews" /></RequireAuth> },
      { path: 'assign', element: <RequireAuth roles={['COORDINATOR']}><AssignPage /></RequireAuth> },
      { path: 'projects/:id/edit', element: <RequireAuth roles={['STUDENT']}><ProjectWizardPage key="edit" /></RequireAuth> },
    ],
  },
  { path: '*', element: <Navigate to="/" replace /> },
]);
