import axios, { AxiosError } from 'axios';
import type { ApiError, AuthResponse } from './types';

const SESSION_KEY = 'prastav.session';

/**
 * The session lives in localStorage when "Remember me" is ticked (survives a browser restart)
 * and in sessionStorage otherwise (ends with the tab).
 */
export function loadSession(): AuthResponse | null {
  const raw = localStorage.getItem(SESSION_KEY) ?? sessionStorage.getItem(SESSION_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as AuthResponse;
  } catch {
    return null;
  }
}

export function saveSession(session: AuthResponse | null, remember = true): void {
  localStorage.removeItem(SESSION_KEY);
  sessionStorage.removeItem(SESSION_KEY);
  if (session) (remember ? localStorage : sessionStorage).setItem(SESSION_KEY, JSON.stringify(session));
}

export const api = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL?.trim() || '/api' });

// Attach the JWT to every request.
api.interceptors.request.use((config) => {
  const session = loadSession();
  if (session) config.headers.Authorization = `Bearer ${session.token}`;
  return config;
});

// An expired or invalid token ends the session; the sign-in page says why and comes back to this page afterwards.
api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    const isLogin = error.config?.url?.startsWith('/auth/');
    if (error.response?.status === 401 && !isLogin) {
      saveSession(null);
      const here = window.location.pathname + window.location.search;
      window.location.assign(`/login?expired=1&next=${encodeURIComponent(here)}`);
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
    if (!error.response) return 'The server is not reachable. Check your connection and try again.';
    if (error.response.status >= 500) return 'The server could not complete this request. Try again in a moment.';
  }
  return 'Something went wrong. Try again.';
}

/** Per-field validation messages from a 400 response, keyed by request field name. */
export function fieldErrors(error: unknown): Record<string, string> {
  if (!axios.isAxiosError(error)) return {};
  const body = error.response?.data as ApiError | undefined;
  return Object.fromEntries((body?.fieldErrors ?? []).map((f) => [f.field, f.message]));
}
