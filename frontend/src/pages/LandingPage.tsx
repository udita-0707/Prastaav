import {
  ArrowRight,
  CheckCircle2,
  ClipboardCheck,
  FilePen,
  FilePlus2,
  FileText,
  History,
  LayoutDashboard,
  Lock,
  MessageSquareText,
  RotateCcw,
  Send,
  ShieldCheck,
  TrendingUp,
  UserSearch,
  Workflow,
  type LucideIcon,
} from 'lucide-react';
import { Navigate } from 'react-router-dom';
import type { ProjectStatus } from '../api/types';
import { brand, illustrations } from '../assets';
import { useAuth } from '../auth/AuthContext';
import { Avatar, ButtonLink, Card, IconTile, Logo, StatusPill, type Tone } from '../components/ui';
import { INSTITUTION_NAME, PRODUCT_NAME, TAGLINE } from '../config/brand';
import { useDocumentTitle } from '../lib/useDocumentTitle';

const NAV = [
  { href: '#home', label: 'Home' },
  { href: '#features', label: 'Features' },
  { href: '#roles', label: 'Roles' },
  { href: '#about', label: 'About' },
];

/** Every card describes something the app does today. */
const FEATURES: { icon: LucideIcon; tone: Tone; title: string; text: string }[] = [
  { icon: Workflow, tone: 'teal', title: 'Structured workflow', text: 'Draft, submit, review, revise, approve: only valid steps, only by the right role.' },
  { icon: ShieldCheck, tone: 'violet', title: 'Role-based access', text: 'Students see their own projects, faculty their assigned ones, the coordinator everything.' },
  { icon: Lock, tone: 'blue', title: 'Versioned proposals', text: 'Every submitted version is frozen. A revision starts as a copy, so earlier versions stay intact.' },
  { icon: MessageSquareText, tone: 'amber', title: 'Faculty feedback', text: 'Approve, request changes or reject, with comments tied to the exact version reviewed.' },
  { icon: History, tone: 'green', title: 'Complete history', text: 'A timeline of versions, decisions and status changes, backed by an append-only audit log.' },
  { icon: LayoutDashboard, tone: 'sky', title: 'Dashboards and search', text: 'Counts by status, filters, search and paging for every role.' },
];

const ROLES = [
  {
    title: 'Student',
    image: illustrations.studentLaptop,
    points: ['Create a project and draft the proposal step by step', 'Submit, then follow its status', 'Read feedback and resubmit a new version'],
  },
  {
    title: 'Faculty',
    image: illustrations.facultyLaptop,
    points: ['See the projects assigned to you', 'Review the latest submitted version', 'Approve, request changes or reject with comments'],
  },
  {
    title: 'Coordinator',
    image: illustrations.coordinatorLaptop,
    points: ['See every project and its status', 'Assign a faculty reviewer to submitted projects', 'Filter and search across all proposals'],
  },
];

const STEPS: { icon: LucideIcon; tone: Tone; title: string; text: string }[] = [
  { icon: FilePlus2, tone: 'amber', title: 'Create', text: 'Write the proposal in four guided steps and save drafts as you go.' },
  { icon: Send, tone: 'sky', title: 'Submit', text: 'Submitting freezes the version and sends it to the coordinator.' },
  { icon: UserSearch, tone: 'violet', title: 'Review', text: 'The coordinator assigns a faculty reviewer, who records a decision.' },
  { icon: RotateCcw, tone: 'green', title: 'Revise or approve', text: 'Revise and resubmit when changes are requested, until approved.' },
];

const PREVIEW_ROWS: { title: string; status: ProjectStatus; date: string }[] = [
  { title: 'Library seat booking', status: 'DRAFT', date: '12 Apr' },
  { title: 'Crop health monitoring', status: 'UNDER_REVIEW', date: '10 Apr' },
  { title: 'Waste sorting robot', status: 'SUBMITTED', date: '8 Apr' },
  { title: 'Course recommender', status: 'APPROVED', date: '5 Apr' },
];

/** A scaled-down picture of the student home built from the real components, with neutral sample data. */
function ProductPreview() {
  const stats: { label: string; value: number; icon: LucideIcon; tone: Tone }[] = [
    { label: 'Total', value: 4, icon: FileText, tone: 'blue' },
    { label: 'Drafts', value: 1, icon: FilePen, tone: 'amber' },
    { label: 'In review', value: 2, icon: Send, tone: 'violet' },
    { label: 'Approved', value: 1, icon: CheckCircle2, tone: 'green' },
  ];
  return (
    <figure className="relative" aria-label="Preview of the student home with sample data">
      <div className="pointer-events-none select-none overflow-hidden rounded-card border border-line bg-white shadow-pop" aria-hidden>
        <div className="flex">
          <div className="hidden w-36 shrink-0 bg-ink p-3 sm:block">
            <span className="flex items-center gap-1.5 text-xs font-bold text-white">
              <img src={brand.logoMark} alt="" className="h-5 w-5" />
              {PRODUCT_NAME}
            </span>
            <ul className="mt-4 space-y-1 text-[10px] font-medium text-slate-300">
              <li className="rounded-md bg-brand-600 px-2 py-1.5 text-white">Home</li>
              <li className="px-2 py-1.5">My Projects</li>
              <li className="px-2 py-1.5">Create Project</li>
            </ul>
          </div>
          <div className="min-w-0 flex-1 bg-canvas p-4">
            <div className="flex items-center justify-between">
              <p className="text-sm font-bold text-ink">Good afternoon, Alex</p>
              <Avatar name="Alex Sample" size="xs" />
            </div>
            <div className="mt-3 grid grid-cols-4 gap-2">
              {stats.map(({ label, value, icon: Icon, tone }) => (
                <div key={label} className="rounded-lg border border-line bg-white p-2">
                  <IconTile tone={tone} size="sm" className="!h-6 !w-6 !rounded-md">
                    <Icon className="h-3.5 w-3.5" />
                  </IconTile>
                  <p className="mt-1.5 text-[9px] text-muted">{label}</p>
                  <p className="text-sm font-bold text-ink">{value}</p>
                </div>
              ))}
            </div>
            <div className="mt-3 rounded-lg border border-line bg-white p-2">
              <p className="px-1 text-[10px] font-bold text-ink">Recent projects</p>
              <ul className="mt-1 divide-y divide-line">
                {PREVIEW_ROWS.map((row) => (
                  <li key={row.title} className="flex items-center justify-between gap-2 px-1 py-1.5 text-[10px]">
                    <span className="truncate font-medium text-ink">{row.title}</span>
                    <span className="flex shrink-0 items-center gap-2">
                      <span className="origin-right scale-[0.8]">
                        <StatusPill status={row.status} size="sm" />
                      </span>
                      <span className="text-muted">{row.date}</span>
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      </div>
      <figcaption className="mt-3 text-center text-xs text-muted">Sample data. Your dashboard shows your own projects.</figcaption>
    </figure>
  );
}

function SectionHeading({ eyebrow, title, text }: { eyebrow: string; title: string; text?: string }) {
  return (
    <div className="max-w-xl">
      <p className="text-xs font-bold uppercase tracking-[0.16em] text-brand-700">{eyebrow}</p>
      <h2 className="mt-2 text-3xl font-bold tracking-tight text-ink">{title}</h2>
      {text && <p className="mt-3 text-muted">{text}</p>}
    </div>
  );
}

export function LandingPage() {
  useDocumentTitle(undefined);
  const { user } = useAuth();
  if (user) return <Navigate to="/app" replace />;

  return (
    <div id="home">
      <header className="sticky top-0 z-30 border-b border-line/60 bg-white/80 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-canvas items-center gap-6 px-4 sm:px-8 lg:px-16">
          <a href="#home" className="rounded-lg" aria-label={`${PRODUCT_NAME} home`}>
            <Logo />
          </a>
          <nav aria-label="Sections" className="mx-auto hidden md:block">
            <ul className="flex gap-8 text-sm font-semibold text-slate-600">
              {NAV.map((item) => (
                <li key={item.href}>
                  <a href={item.href} className="hover:text-ink">
                    {item.label}
                  </a>
                </li>
              ))}
            </ul>
          </nav>
          <div className="ml-auto flex items-center gap-2 md:ml-0">
            <ButtonLink to="/login" variant="ghost" size="sm">
              Login
            </ButtonLink>
            <ButtonLink to="/register" size="sm" trailingIcon={<ArrowRight className="h-4 w-4" aria-hidden />}>
              Get Started
            </ButtonLink>
          </div>
        </div>
      </header>

      <main>
        <section className="mx-auto grid max-w-canvas items-center gap-12 px-4 py-14 sm:px-8 lg:grid-cols-2 lg:px-16 lg:py-20">
          <div>
            <span className="inline-flex rounded-full bg-brand-50 px-3 py-1 text-xs font-semibold text-brand-700 ring-1 ring-inset ring-brand-100">Academic Project Proposal Platform</span>
            <h1 className="mt-5 text-4xl font-extrabold leading-[1.1] tracking-tight text-ink sm:text-5xl lg:text-[3.4rem]">
              Turn your ideas into
              <span className="block bg-brand-text bg-clip-text text-transparent">impactful projects</span>
            </h1>
            <p className="mt-6 max-w-xl text-lg leading-relaxed text-muted">
              {PRODUCT_NAME} is one place for students, faculty and coordinators to create, review and approve academic project proposals.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <ButtonLink to="/register" size="lg" trailingIcon={<ArrowRight className="h-4 w-4" aria-hidden />}>
                Get Started
              </ButtonLink>
              <a href="#how-it-works" className="inline-flex h-12 items-center gap-2 rounded-field border border-line bg-white px-6 font-semibold text-ink hover:bg-slate-50">
                See how it works
              </a>
            </div>
            <ul className="mt-10 flex flex-wrap gap-x-8 gap-y-4">
              {[
                { icon: FilePlus2, label: 'Create proposals', tone: 'teal' as Tone },
                { icon: TrendingUp, label: 'Track progress', tone: 'violet' as Tone },
                { icon: ClipboardCheck, label: 'Get faculty feedback', tone: 'blue' as Tone },
              ].map(({ icon: Icon, label, tone }) => (
                <li key={label} className="flex items-center gap-3 text-sm font-medium text-ink">
                  <IconTile tone={tone} size="sm">
                    <Icon className="h-4 w-4" />
                  </IconTile>
                  {label}
                </li>
              ))}
            </ul>
          </div>
          <ProductPreview />
        </section>

        <section id="features" className="scroll-mt-20 border-y border-line/60 bg-white/70">
          <div className="mx-auto grid max-w-canvas gap-10 px-4 py-16 sm:px-8 lg:grid-cols-[20rem_1fr] lg:px-16">
            <SectionHeading eyebrow={`Why ${PRODUCT_NAME}`} title="Everything a proposal needs, in one flow" text="From the first draft to the final decision, every step is recorded and every role sees what it needs." />
            <ul className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
              {FEATURES.map(({ icon: Icon, tone, title, text }) => (
                <li key={title}>
                  <Card className="h-full p-5">
                    <IconTile tone={tone}>
                      <Icon className="h-5 w-5" />
                    </IconTile>
                    <h3 className="mt-4 font-bold text-ink">{title}</h3>
                    <p className="mt-1.5 text-sm leading-relaxed text-muted">{text}</p>
                  </Card>
                </li>
              ))}
            </ul>
          </div>
        </section>

        <section id="roles" className="mx-auto max-w-canvas scroll-mt-20 px-4 py-16 sm:px-8 lg:px-16">
          <SectionHeading eyebrow="Built for everyone" title="Different roles. Same goal." text={`${PRODUCT_NAME} brings students, faculty and coordinators together on a single platform.`} />
          <ul className="mt-10 grid gap-6 md:grid-cols-3">
            {ROLES.map((role) => (
              <li key={role.title}>
                <Card className="h-full overflow-hidden">
                  <div className="bg-gradient-to-b from-brand-50/70 to-white px-6 pt-6">
                    <img src={role.image} alt="" className="mx-auto h-40 w-auto" />
                  </div>
                  <div className="p-6">
                    <h3 className="text-lg font-bold text-ink">{role.title}</h3>
                    <ul className="mt-3 space-y-2">
                      {role.points.map((point) => (
                        <li key={point} className="flex items-start gap-2 text-sm text-slate-700">
                          <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0 text-brand-600" aria-hidden />
                          {point}
                        </li>
                      ))}
                    </ul>
                  </div>
                </Card>
              </li>
            ))}
          </ul>
        </section>

        <section id="how-it-works" className="scroll-mt-20 border-y border-line/60 bg-white/70">
          <div className="mx-auto max-w-canvas px-4 py-16 sm:px-8 lg:px-16">
            <SectionHeading eyebrow="How it works" title="Simple steps. Clear decisions." />
            <ol className="mt-10 grid gap-8 sm:grid-cols-2 lg:grid-cols-4">
              {STEPS.map(({ icon: Icon, tone, title, text }, index) => (
                <li key={title} className="relative">
                  <div className="flex items-center gap-3">
                    <span className="flex h-7 w-7 items-center justify-center rounded-full bg-ink text-xs font-bold text-white">{index + 1}</span>
                    <IconTile tone={tone} size="lg">
                      <Icon className="h-6 w-6" />
                    </IconTile>
                  </div>
                  <h3 className="mt-4 font-bold text-ink">{title}</h3>
                  <p className="mt-1.5 text-sm leading-relaxed text-muted">{text}</p>
                </li>
              ))}
            </ol>
          </div>
        </section>

        <section id="about" className="mx-auto max-w-canvas scroll-mt-20 px-4 py-16 sm:px-8 lg:px-16">
          <div className="grid items-center gap-8 lg:grid-cols-[1fr_auto]">
            <SectionHeading
              eyebrow={`About ${PRODUCT_NAME}`}
              title={TAGLINE}
              text="Proposal reviews often run over email and spreadsheets, where versions get lost and feedback drifts from the text it refers to. Prastaav keeps every version, decision and comment together, with a workflow that only allows valid steps."
            />
            <img src={illustrations.ideaBulb} alt="" className="hidden h-36 w-auto lg:block" />
          </div>
          <div className="mt-10 flex flex-col items-start gap-5 rounded-card bg-gradient-to-r from-brand-800 via-brand-700 to-accent-700 px-6 py-8 text-white sm:flex-row sm:items-center sm:px-10">
            <Logo className="[&_span]:!text-white" />
            <div className="min-w-0 flex-1">
              <h2 className="text-xl font-bold">Ready to start your next project?</h2>
              <p className="mt-1 text-sm text-white/85">Create an account and turn your idea into a proposal today.</p>
            </div>
            <ButtonLink to="/register" variant="outline" className="border-white bg-white text-brand-800 hover:bg-white/90" trailingIcon={<ArrowRight className="h-4 w-4" aria-hidden />}>
              Get Started
            </ButtonLink>
          </div>
        </section>
      </main>

      <footer className="border-t border-line/60 bg-white/70">
        <div className="mx-auto flex max-w-canvas flex-col gap-4 px-4 py-6 text-sm text-muted sm:flex-row sm:items-center sm:justify-between sm:px-8 lg:px-16">
          <p>
            © {new Date().getFullYear()} {INSTITUTION_NAME} · {PRODUCT_NAME}
          </p>
          <nav aria-label="Footer">
            <ul className="flex gap-6">
              {NAV.map((item) => (
                <li key={item.href}>
                  <a href={item.href} className="hover:text-ink">
                    {item.label}
                  </a>
                </li>
              ))}
            </ul>
          </nav>
        </div>
      </footer>
    </div>
  );
}
