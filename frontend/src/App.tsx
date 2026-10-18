import { Navigate, Route, Routes } from 'react-router-dom';
import { RequireAuth } from './auth/RequireAuth';
import { Layout } from './components/Layout';
import { CreateProjectPage } from './pages/CreateProjectPage';
import { LoginPage } from './pages/LoginPage';
import { ProjectDetailPage } from './pages/ProjectDetailPage';
import { ProjectsPage } from './pages/ProjectsPage';
import { ProposalEditorPage } from './pages/ProposalEditorPage';
import { RegisterPage } from './pages/RegisterPage';

export function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/" element={<RequireAuth><ProjectsPage /></RequireAuth>} />
        <Route path="/projects/new" element={<RequireAuth roles={['STUDENT']}><CreateProjectPage /></RequireAuth>} />
        <Route path="/projects/:id" element={<RequireAuth><ProjectDetailPage /></RequireAuth>} />
        <Route path="/projects/:id/edit" element={<RequireAuth roles={['STUDENT']}><ProposalEditorPage /></RequireAuth>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
