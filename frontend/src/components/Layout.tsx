import { Link, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { ROLE_LABEL } from './labels';

export function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  return (
    <>
      <header className="topbar">
        <Link to="/" className="wordmark">
          Prastav
        </Link>
        {user && (
          <div className="whoami">
            <span>
              {user.name} <span className="muted">· {ROLE_LABEL[user.role]}</span>
            </span>
            <button
              className="link-button"
              onClick={() => {
                logout();
                navigate('/login');
              }}
            >
              Sign out
            </button>
          </div>
        )}
      </header>
      <main className="page">
        <Outlet />
      </main>
    </>
  );
}
