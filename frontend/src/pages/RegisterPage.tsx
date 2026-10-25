import { ArrowRight, Eye, EyeOff, Lock, Mail, UserRound } from 'lucide-react';
import { useState } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { errorMessage, fieldErrors } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { ErrorNote } from '../components/ErrorNote';
import { Button, Card, Input } from '../components/ui';
import { INSTITUTION_EMAIL_DOMAIN, PRODUCT_NAME } from '../config/brand';
import { AuthLayout } from '../layouts/AuthLayout';
import { useDocumentTitle } from '../lib/useDocumentTitle';

export function RegisterPage() {
  useDocumentTitle('Create an account');
  const { user, register } = useAuth();
  const navigate = useNavigate();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/app" replace />;

  const mismatch = confirm.length > 0 && confirm !== password ? 'Passwords do not match.' : null;

  return (
    <AuthLayout>
      <Card className="p-6 sm:p-10">
        <h2 className="text-3xl font-bold tracking-tight text-ink">Create your account</h2>
        <p className="mt-2 text-muted">
          Join {PRODUCT_NAME} as a student. Faculty and coordinator accounts are created by your coordinator.
        </p>
        <form
          className="mt-8 space-y-5"
          onSubmit={async (e) => {
            e.preventDefault();
            if (mismatch) return;
            setBusy(true);
            setError(null);
            setFields({});
            try {
              await register(name, email, password);
              navigate('/app', { replace: true });
            } catch (err) {
              const byField = fieldErrors(err);
              setFields(byField);
              setError(Object.keys(byField).length > 0 ? 'Please correct the highlighted fields.' : errorMessage(err));
            } finally {
              setBusy(false);
            }
          }}
        >
          <Input
            label="Full name"
            autoComplete="name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            icon={<UserRound className="h-4 w-4" />}
            maxLength={120}
            error={fields.name}
            required
            autoFocus
          />
          <Input
            label="Student email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder={`firstname.23csai@${INSTITUTION_EMAIL_DOMAIN}`}
            icon={<Mail className="h-4 w-4" />}
            hint={`Your institutional student email: firstname, admission year and branch, e.g. aarav.23csai@${INSTITUTION_EMAIL_DOMAIN}.`}
            maxLength={255}
            error={fields.email}
            required
          />
          <div className="grid gap-5 sm:grid-cols-2">
            <Input
              label="Password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="new-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              icon={<Lock className="h-4 w-4" />}
              hint="8 to 72 characters."
              minLength={8}
              maxLength={72}
              error={fields.password}
              required
              trailing={
                <button
                  type="button"
                  onClick={() => setShowPassword((s) => !s)}
                  className="rounded-lg p-2 text-slate-400 hover:text-ink"
                  aria-label={showPassword ? 'Hide passwords' : 'Show passwords'}
                  aria-pressed={showPassword}
                >
                  {showPassword ? <EyeOff className="h-4 w-4" aria-hidden /> : <Eye className="h-4 w-4" aria-hidden />}
                </button>
              }
            />
            <Input
              label="Confirm password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="new-password"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              icon={<Lock className="h-4 w-4" />}
              error={mismatch}
              required
            />
          </div>
          <ErrorNote message={error} />
          <Button type="submit" variant="gradient" size="lg" className="w-full" loading={busy} disabled={Boolean(mismatch)} trailingIcon={<ArrowRight className="h-4 w-4" aria-hidden />}>
            Create account
          </Button>
        </form>
        <p className="mt-6 text-center text-sm text-muted">
          Already registered?{' '}
          <Link to="/login" className="font-semibold text-accent-700 hover:underline">
            Sign in
          </Link>
        </p>
      </Card>
    </AuthLayout>
  );
}
