import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { errorMessage } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { ErrorNote } from '../components/ErrorNote';

const DEMO_ACCOUNTS = [
  ['Coordinator', 'coordinator@prastav.edu'],
  ['Faculty', 'anita.rao@prastav.edu'],
  ['Student', 'aarav@prastav.edu'],
];

export function LoginPage() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/" replace />;

  return (
    <section className="auth-card">
      <h1>Sign in to Prastav</h1>
      <form
        className="stack"
        onSubmit={async (e) => {
          e.preventDefault();
          setBusy(true);
          setError(null);
          try {
            await login(email, password);
            navigate('/');
          } catch (err) {
            setError(errorMessage(err));
          } finally {
            setBusy(false);
          }
        }}
      >
        <label className="field">
          <span>Email</span>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoFocus />
        </label>
        <label className="field">
          <span>Password</span>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </label>
        <button className="primary" disabled={busy}>
          Sign in
        </button>
        <ErrorNote message={error} />
      </form>
      <p className="muted">
        New student or faculty member? <Link to="/register">Create an account</Link>
      </p>
      <details className="demo">
        <summary>Demo accounts</summary>
        <p className="muted">All seeded accounts use the password Password@123.</p>
        <ul>
          {DEMO_ACCOUNTS.map(([role, address]) => (
            <li key={address}>
              <button
                type="button"
                className="link-button"
                onClick={() => {
                  setEmail(address);
                  setPassword('Password@123');
                }}
              >
                {role}: {address}
              </button>
            </li>
          ))}
        </ul>
      </details>
    </section>
  );
}
