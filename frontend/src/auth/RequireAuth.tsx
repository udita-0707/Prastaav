import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import type { Role } from '../api/types';
import { ButtonLink, Card, EmptyState } from '../components/ui';
import { useAuth } from './AuthContext';

/** Route guard: requires a session and, optionally, one of the given roles. Remembers where a signed-out user was going. */
export function RequireAuth({ roles, children }: { roles?: Role[]; children: ReactNode }) {
  const { user } = useAuth();
  const location = useLocation();
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  if (roles && !roles.includes(user.role)) {
    return (
      <Card>
        <EmptyState
          title="You do not have access to this page"
          description="This page is for a different role. Your own work is on your home page."
          action={<ButtonLink to="/app">Go to home</ButtonLink>}
        />
      </Card>
    );
  }
  return <>{children}</>;
}
