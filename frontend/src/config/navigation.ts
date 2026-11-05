import { ClipboardCheck, FolderKanban, Home, LayoutDashboard, PlusCircle, ScrollText, UserPlus, type LucideIcon } from 'lucide-react';
import type { Role } from '../api/types';

export interface NavItem {
  label: string;
  to: string;
  icon: LucideIcon;
}

const HOME: NavItem = { label: 'Home', to: '/app', icon: Home };

/** Sidebar entries per role. Routes are guarded separately in router.tsx. */
export const NAV_ITEMS: Record<Role, NavItem[]> = {
  STUDENT: [
    HOME,
    { label: 'My Projects', to: '/app/projects', icon: FolderKanban },
    { label: 'Create Project', to: '/app/projects/new', icon: PlusCircle },
  ],
  FACULTY: [
    HOME,
    { label: 'Assigned Projects', to: '/app/projects', icon: FolderKanban },
    { label: 'Reviews', to: '/app/reviews', icon: ClipboardCheck },
  ],
  COORDINATOR: [
    HOME,
    { label: 'Dashboard', to: '/app/dashboard', icon: LayoutDashboard },
    { label: 'All Projects', to: '/app/projects', icon: FolderKanban },
    { label: 'Assign Faculty', to: '/app/assign', icon: UserPlus },
    { label: 'Reviews', to: '/app/reviews', icon: ClipboardCheck },
    { label: 'Audit Log', to: '/app/audit', icon: ScrollText },
  ],
};

export const SEARCH_PLACEHOLDER: Record<Role, string> = {
  STUDENT: 'Search your projects…',
  FACULTY: 'Search projects, students or keywords…',
  COORDINATOR: 'Search projects, teams or faculty…',
};

/** "/app/projects" stays highlighted on a project page, but not on the create wizard (its own entry). */
export function isNavActive(to: string, pathname: string): boolean {
  if (to === '/app') return pathname === '/app';
  if (to === '/app/projects') return pathname === to || (pathname.startsWith('/app/projects/') && pathname !== '/app/projects/new');
  return pathname === to || pathname.startsWith(`${to}/`);
}
