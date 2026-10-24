import axios, { AxiosError } from 'axios';
import type { ApiError, AuthResponse } from './types';

const SESSION_KEY = 'prastav.session';

export function loadSession(): AuthResponse | null {
  const raw = localStorage.getItem(SESSION_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthResponse;
  } catch {
    return null;
  }
}

export function saveSession(session: AuthResponse | null): void {
  if (session) localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  else localStorage.removeItem(SESSION_KEY);
}

export const api = axios.create({ baseURL: '/api' });

// Attach the JWT to every request.
api.interceptors.request.use((config) => {
  const session = loadSession();
  if (session) config.headers.Authorization = `Bearer ${session.token}`;
  return config;
});

// An expired or invalid token ends the session.
api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const isLogin = error.config?.url?.startsWith('/auth/');
    if (error.response?.status === 401 && !isLogin) {
      saveSession(null);
      window.location.assign('/login');
    }
    return Promise.reject(error);
  },
);

/** Turns any failed call into one readable sentence, using the backend's ApiError body when present. */
export function errorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const body = error.response?.data as ApiError | undefined;
    if (body?.message) {
      const fields = body.fieldErrors?.map((f) => `${f.field}: ${f.message}`).join('; ');
      return fields ? `${body.message} (${fields})` : body.message;
    }
    if (!error.response) return 'The server is not reachable. Is the backend running on port 8080?';
  }
  return 'Something went wrong. Try again.';
}
