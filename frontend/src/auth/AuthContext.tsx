import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { loadSession, saveSession } from '../api/client';
import * as endpoints from '../api/endpoints';
import type { Role, UserResponse } from '../api/types';

interface AuthState {
  user: UserResponse | null;
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string, role: Role) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(() => loadSession()?.user ?? null);

  const login = useCallback(async (email: string, password: string) => {
    const session = await endpoints.login(email, password);
    saveSession(session);
    setUser(session.user);
  }, []);

  const register = useCallback(async (name: string, email: string, password: string, role: Role) => {
    const session = await endpoints.register(name, email, password, role);
    saveSession(session);
    setUser(session.user);
  }, []);

  const logout = useCallback(() => {
    saveSession(null);
    setUser(null);
  }, []);

  const value = useMemo(() => ({ user, login, register, logout }), [user, login, register, logout]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
