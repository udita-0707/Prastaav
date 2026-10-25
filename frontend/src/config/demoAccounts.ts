import type { Role } from '../api/types';
import { INSTITUTION_EMAIL_DOMAIN } from './brand';

/** Mirrors prastav.seed.demo-password, the one password the backend's DemoDataSeeder gives every demo account. */
export const DEMO_PASSWORD = 'Password@123';

/** Mirrors the accounts created by the backend's DemoDataSeeder (prastav.seed.enabled). */
export const DEMO_ACCOUNTS: { role: Role; name: string; email: string }[] = [
  { role: 'STUDENT', name: 'Aarav Mehta', email: `aarav.23csai@${INSTITUTION_EMAIL_DOMAIN}` },
  { role: 'STUDENT', name: 'Diya Kapoor', email: `diya.23csai@${INSTITUTION_EMAIL_DOMAIN}` },
  { role: 'STUDENT', name: 'Rohan Iyer', email: `rohan.24csai@${INSTITUTION_EMAIL_DOMAIN}` },
  { role: 'FACULTY', name: 'Dr. Anita Rao', email: `anita.rao@${INSTITUTION_EMAIL_DOMAIN}` },
  { role: 'FACULTY', name: 'Dr. Vikram Shah', email: `vikram.shah@${INSTITUTION_EMAIL_DOMAIN}` },
  { role: 'COORDINATOR', name: 'Meera Nair', email: `coordinator@${INSTITUTION_EMAIL_DOMAIN}` },
];

/** Hide the demo buttons with VITE_SHOW_DEMO_ACCOUNTS=false when the backend runs without seed data. */
export const SHOW_DEMO_ACCOUNTS = import.meta.env.VITE_SHOW_DEMO_ACCOUNTS !== 'false';
