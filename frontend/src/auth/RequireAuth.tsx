import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import type { Role } from '../api/types';
import { useAuth } from './AuthContext';

/** Route guard: requires a session and, optionally, one of the given roles. */
export function RequireAuth({ roles, children }: { roles?: Role[]; children: ReactNode }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/" replace />;
  return <>{children}</>;
}
