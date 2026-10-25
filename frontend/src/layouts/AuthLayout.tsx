import { FilePlus2, MessageSquareText, TrendingUp } from 'lucide-react';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { illustrations } from '../assets';
import { Logo } from '../components/ui';
import { PRODUCT_NAME, TAGLINE } from '../config/brand';

const POINTS = [
  { icon: FilePlus2, label: 'Create proposals' },
  { icon: TrendingUp, label: 'Track progress' },
  { icon: MessageSquareText, label: 'Get faculty feedback' },
];

/** Split layout shared by sign in and registration: product story on the left, the form card on the right. */
export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="mx-auto grid min-h-screen max-w-canvas items-center gap-10 px-4 py-8 sm:px-8 lg:grid-cols-[1fr_minmax(0,34rem)] lg:gap-16 lg:px-16">
      <div className="flex flex-col">
        <Link to="/" className="self-start rounded-lg" aria-label={`${PRODUCT_NAME} home`}>
          <Logo size="lg" />
        </Link>
        <div className="mt-10 hidden lg:block">
          <h1 className="text-5xl font-extrabold leading-[1.1] tracking-tight text-ink">
            Turn ideas into
            <span className="block bg-brand-text bg-clip-text text-transparent">impactful projects</span>
          </h1>
          <p className="mt-6 max-w-lg text-lg leading-relaxed text-muted">
            One place for students, faculty and coordinators to write project proposals, review them and see every decision.
          </p>
          <ul className="mt-8 flex flex-wrap gap-x-8 gap-y-4">
            {POINTS.map(({ icon: Icon, label }) => (
              <li key={label} className="flex items-center gap-3 text-sm font-medium text-ink">
                <span className="flex h-11 w-11 items-center justify-center rounded-full bg-white text-accent-600 shadow-card">
                  <Icon className="h-5 w-5" aria-hidden />
                </span>
                {label}
              </li>
            ))}
          </ul>
          <img src={illustrations.studentLaptop} alt="" className="mt-10 w-full max-w-md" />
          <p className="mt-4 text-sm font-semibold text-brand-700">{TAGLINE}</p>
        </div>
      </div>
      <main className="w-full">{children}</main>
    </div>
  );
}
