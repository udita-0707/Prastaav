import type { Role } from '../api/types';

/** Mirrors the accounts created by the backend's DemoDataSeeder (prastav.seed.enabled). */
export const DEMO_PASSWORD = 'Password@123';

export const DEMO_ACCOUNTS: { role: Role; label: string; email: string }[] = [
  { role: 'STUDENT', label: 'Student', email: 'aarav@prastav.edu' },
  { role: 'FACULTY', label: 'Faculty', email: 'anita.rao@prastav.edu' },
  { role: 'COORDINATOR', label: 'Coordinator', email: 'coordinator@prastav.edu' },
];

/** Hide the demo buttons with VITE_SHOW_DEMO_ACCOUNTS=false when the backend runs without seed data. */
export const SHOW_DEMO_ACCOUNTS = import.meta.env.VITE_SHOW_DEMO_ACCOUNTS !== 'false';
