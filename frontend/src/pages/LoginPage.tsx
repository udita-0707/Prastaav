import { ArrowRight, Eye, EyeOff, GraduationCap, Lock, Mail, UserRound, UsersRound, type LucideIcon } from 'lucide-react';
import { useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { errorMessage } from '../api/client';
import type { Role } from '../api/types';
import { ROLE_LABEL } from '../components/labels';
import { useAuth } from '../auth/AuthContext';
import { ErrorNote } from '../components/ErrorNote';
import { Button, ButtonLink, Card, Input } from '../components/ui';
import { PRODUCT_NAME } from '../config/brand';
import { DEMO_ACCOUNTS, DEMO_PASSWORD, SHOW_DEMO_ACCOUNTS } from '../config/demoAccounts';
import { AuthLayout } from '../layouts/AuthLayout';
import { useDocumentTitle } from '../lib/useDocumentTitle';

const DEMO_ICON: Record<Role, LucideIcon> = { STUDENT: GraduationCap, FACULTY: UserRound, COORDINATOR: UsersRound };

export function LoginPage() {
  useDocumentTitle('Sign in');
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? '/app';
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [remember, setRemember] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to={from} replace />;

  return (
    <AuthLayout>
      <Card className="p-6 sm:p-10">
        <h2 className="text-3xl font-bold tracking-tight text-ink">Welcome back!</h2>
        <p className="mt-2 text-muted">Sign in to continue to {PRODUCT_NAME}</p>
        <form
          className="mt-8 space-y-5"
          noValidate={false}
          onSubmit={async (e) => {
            e.preventDefault();
            setBusy(true);
            setError(null);
            try {
              await login(email, password, remember);
              navigate(from, { replace: true });
            } catch (err) {
              setError(errorMessage(err));
            } finally {
              setBusy(false);
            }
          }}
        >
          <Input
            label="Email address"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="youremail@college.edu"
            icon={<Mail className="h-4 w-4" />}
            required
            autoFocus
          />
          <Input
            label="Password"
            type={showPassword ? 'text' : 'password'}
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="Enter your password"
            icon={<Lock className="h-4 w-4" />}
            required
            trailing={
              <button
                type="button"
                onClick={() => setShowPassword((s) => !s)}
                className="rounded-lg p-2 text-slate-400 hover:text-ink"
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                aria-pressed={showPassword}
              >
                {showPassword ? <EyeOff className="h-4 w-4" aria-hidden /> : <Eye className="h-4 w-4" aria-hidden />}
              </button>
            }
          />
          <label className="flex w-fit cursor-pointer items-center gap-2.5 text-sm text-ink">
            <input
              type="checkbox"
              checked={remember}
              onChange={(e) => setRemember(e.target.checked)}
              className="h-4 w-4 rounded border-slate-300 text-brand-600 accent-brand-600"
            />
            Remember me
          </label>
          <ErrorNote message={error} />
          <Button type="submit" variant="gradient" size="lg" className="w-full" loading={busy} trailingIcon={<ArrowRight className="h-4 w-4" aria-hidden />}>
            Sign in
          </Button>
        </form>

        <div className="my-6 flex items-center gap-4 text-sm text-muted" aria-hidden>
          <span className="h-px flex-1 bg-line" />
          or
          <span className="h-px flex-1 bg-line" />
        </div>
        <ButtonLink to="/register" variant="outline" size="lg" className="w-full">
          Create an account
        </ButtonLink>

        {SHOW_DEMO_ACCOUNTS && (
          <section className="mt-8" aria-labelledby="demo-heading">
            <h3 id="demo-heading" className="text-sm font-semibold text-ink">
              Try demo accounts
            </h3>
            <p className="mt-1 text-xs text-muted">Fills in the form; then press Sign in.</p>
            <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2">
              {DEMO_ACCOUNTS.map(({ role, name, email: demoEmail }) => {
                const Icon = DEMO_ICON[role];
                return (
                  <button
                    key={demoEmail}
                    type="button"
                    onClick={() => {
                      setEmail(demoEmail);
                      setPassword(DEMO_PASSWORD);
                      setError(null);
                    }}
                    className="flex items-center gap-3 rounded-field border border-line bg-white px-3 py-2.5 text-left text-sm font-medium text-ink transition-colors hover:border-brand-300 hover:bg-brand-50"
                    aria-label={`Fill in the demo account of ${name}, ${ROLE_LABEL[role].toLowerCase()}`}
                  >
                    <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-accent-50 text-accent-600">
                      <Icon className="h-4 w-4" aria-hidden />
                    </span>
                    <span className="min-w-0">
                      <span className="block truncate">{name}</span>
                      <span className="block text-xs font-normal text-muted">{ROLE_LABEL[role]}</span>
                    </span>
                  </button>
                );
              })}
            </div>
          </section>
        )}
      </Card>
    </AuthLayout>
  );
}
