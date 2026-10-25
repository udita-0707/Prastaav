import clsx from 'clsx';
import { ChevronDown, LogOut, Menu, Search, X } from 'lucide-react';
import { useEffect, useState, type FormEvent } from 'react';
import { Link, Outlet, ScrollRestoration, useLocation, useNavigate } from 'react-router-dom';
import type { UserResponse } from '../api/types';
import { illustrations } from '../assets';
import { useAuth } from '../auth/AuthContext';
import { ROLE_LABEL } from '../components/labels';
import { Avatar, DropdownMenu, Logo } from '../components/ui';
import { isNavActive, NAV_ITEMS, SEARCH_PLACEHOLDER } from '../config/navigation';

function Sidebar({ user, onNavigate }: { user: UserResponse; onNavigate?: () => void }) {
  const { pathname } = useLocation();
  return (
    <div className="flex h-full flex-col">
      <div className="px-6 pb-6 pt-6">
        <Link to="/app" onClick={onNavigate} className="rounded-lg">
          <Logo withTagline />
        </Link>
      </div>
      <nav aria-label="Main" className="flex-1 px-3">
        <ul className="space-y-1">
          {NAV_ITEMS[user.role].map(({ label, to, icon: Icon }) => {
            const active = isNavActive(to, pathname);
            return (
              <li key={to}>
                <Link
                  to={to}
                  onClick={onNavigate}
                  aria-current={active ? 'page' : undefined}
                  className={clsx(
                    'flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-semibold transition-colors',
                    active ? 'bg-brand-50 text-brand-700' : 'text-slate-600 hover:bg-slate-50 hover:text-ink',
                  )}
                >
                  <Icon className={clsx('h-5 w-5', active ? 'text-brand-600' : 'text-slate-400')} aria-hidden />
                  {label}
                </Link>
              </li>
            );
          })}
        </ul>
      </nav>
      <img src={illustrations.sidebarLeaves} alt="" className="pointer-events-none mt-6 w-full opacity-80" />
      <div className="flex items-center gap-3 border-t border-line px-5 py-4">
        <Avatar name={user.name} />
        <div className="min-w-0">
          <p className="truncate text-sm font-semibold text-ink">{user.name}</p>
          <p className="text-xs text-muted">{ROLE_LABEL[user.role]}</p>
        </div>
      </div>
    </div>
  );
}

function GlobalSearch({ user }: { user: UserResponse }) {
  const navigate = useNavigate();
  const { pathname, search } = useLocation();
  const [q, setQ] = useState('');
  // Mirror the list's own search box while on the projects list.
  useEffect(() => {
    setQ(pathname === '/app/projects' ? new URLSearchParams(search).get('q') ?? '' : '');
  }, [pathname, search]);
  const submit = (event: FormEvent) => {
    event.preventDefault();
    const term = q.trim();
    navigate(term ? `/app/projects?q=${encodeURIComponent(term)}` : '/app/projects');
  };
  return (
    <form role="search" onSubmit={submit} className="relative w-full max-w-xl">
      <Search className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" aria-hidden />
      <input
        type="search"
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder={SEARCH_PLACEHOLDER[user.role]}
        aria-label="Search projects"
        className="h-11 w-full rounded-field border border-line bg-white pl-11 pr-4 text-sm text-ink placeholder:text-slate-400 focus:border-brand-500 focus:shadow-focus focus:outline-none"
      />
    </form>
  );
}

/** Signed-in frame: sidebar (a drawer below lg), top bar with search and the user menu, then the page. */
export function AppShell() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const [drawer, setDrawer] = useState(false);

  useEffect(() => setDrawer(false), [pathname]);
  useEffect(() => {
    if (!drawer) return;
    const onKey = (event: KeyboardEvent) => event.key === 'Escape' && setDrawer(false);
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [drawer]);

  if (!user) return null;

  return (
    <div className="min-h-screen lg:pl-64">
      <ScrollRestoration />
      <a href="#main" className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[70] focus:rounded-lg focus:bg-white focus:px-4 focus:py-2 focus:shadow-pop">
        Skip to content
      </a>
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-64 border-r border-line bg-white/90 backdrop-blur lg:block">
        <Sidebar user={user} />
      </aside>

      {drawer && (
        <div className="fixed inset-0 z-40 lg:hidden" role="dialog" aria-modal="true" aria-label="Navigation">
          <div className="absolute inset-0 bg-ink/40" onClick={() => setDrawer(false)} aria-hidden />
          <aside className="absolute inset-y-0 left-0 w-72 max-w-[85vw] bg-white shadow-pop">
            <button type="button" onClick={() => setDrawer(false)} className="absolute right-3 top-6 rounded-lg p-2 text-slate-500 hover:bg-slate-100" aria-label="Close navigation">
              <X className="h-5 w-5" aria-hidden />
            </button>
            <Sidebar user={user} onNavigate={() => setDrawer(false)} />
          </aside>
        </div>
      )}

      <header className="sticky top-0 z-20 border-b border-line/80 bg-white/80 backdrop-blur">
        <div className="mx-auto flex h-[72px] max-w-canvas items-center gap-3 px-4 sm:px-6 lg:px-8">
          <button type="button" onClick={() => setDrawer(true)} className="rounded-lg p-2 text-slate-600 hover:bg-slate-100 lg:hidden" aria-label="Open navigation">
            <Menu className="h-5 w-5" aria-hidden />
          </button>
          <GlobalSearch user={user} />
          <div className="ml-auto">
            <DropdownMenu
              label={`Account menu for ${user.name}`}
              triggerClassName="flex items-center gap-3 rounded-xl px-2 py-1.5 hover:bg-slate-50"
              trigger={
                <>
                  <Avatar name={user.name} />
                  <span className="hidden text-left sm:block">
                    <span className="block text-sm font-semibold text-ink">{user.name}</span>
                    <span className="block text-xs text-muted">{ROLE_LABEL[user.role]}</span>
                  </span>
                  <ChevronDown className="hidden h-4 w-4 text-slate-400 sm:block" aria-hidden />
                </>
              }
              items={[
                {
                  label: 'Sign out',
                  icon: <LogOut className="h-4 w-4" />,
                  onSelect: () => {
                    logout();
                    navigate('/login');
                  },
                },
              ]}
            />
          </div>
        </div>
      </header>

      <main id="main" className="mx-auto max-w-canvas px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
        <Outlet />
      </main>
    </div>
  );
}
