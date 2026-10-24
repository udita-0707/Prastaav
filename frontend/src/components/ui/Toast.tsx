import clsx from 'clsx';
import { CheckCircle2, Info, X, XCircle } from 'lucide-react';
import { createContext, useCallback, useContext, useMemo, useRef, useState, type ReactNode } from 'react';

type ToastTone = 'success' | 'error' | 'info';

interface ToastMessage {
  id: number;
  tone: ToastTone;
  title: string;
  description?: string;
}

interface ToastApi {
  show: (toast: Omit<ToastMessage, 'id'>) => void;
}

const ToastContext = createContext<ToastApi | null>(null);

const ICON = { success: CheckCircle2, error: XCircle, info: Info } as const;
const TONE = { success: 'text-green-600', error: 'text-rose-600', info: 'text-accent-600' } as const;

/** Success and info toasts go to a polite live region; errors are announced assertively. */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastMessage[]>([]);
  const nextId = useRef(1);

  const dismiss = useCallback((id: number) => setToasts((all) => all.filter((t) => t.id !== id)), []);

  const show = useCallback(
    (toast: Omit<ToastMessage, 'id'>) => {
      const id = nextId.current++;
      setToasts((all) => [...all.slice(-2), { ...toast, id }]);
      window.setTimeout(() => dismiss(id), 5000);
    },
    [dismiss],
  );

  const api = useMemo(() => ({ show }), [show]);

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div className="pointer-events-none fixed inset-x-4 bottom-4 z-[60] flex flex-col items-end gap-2 sm:left-auto sm:right-6 sm:w-96">
        {(['polite', 'assertive'] as const).map((politeness) => (
          <div key={politeness} aria-live={politeness} className="flex w-full flex-col gap-2">
            {toasts
              .filter((t) => (t.tone === 'error' ? 'assertive' : 'polite') === politeness)
              .map((toast) => {
                const Icon = ICON[toast.tone];
                return (
                  <div key={toast.id} role="status" className="pointer-events-auto flex items-start gap-3 rounded-xl border border-line bg-white p-4 shadow-pop">
                    <Icon className={clsx('mt-0.5 h-5 w-5 shrink-0', TONE[toast.tone])} aria-hidden />
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-semibold text-ink">{toast.title}</p>
                      {toast.description && <p className="mt-0.5 text-sm text-muted">{toast.description}</p>}
                    </div>
                    <button type="button" onClick={() => dismiss(toast.id)} className="rounded-md p-0.5 text-slate-400 hover:text-ink" aria-label="Dismiss notification">
                      <X className="h-4 w-4" aria-hidden />
                    </button>
                  </div>
                );
              })}
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}

export function useToast(): ToastApi {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used inside ToastProvider');
  return ctx;
}
