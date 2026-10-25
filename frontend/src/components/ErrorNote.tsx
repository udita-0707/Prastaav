import { AlertCircle, RotateCw } from 'lucide-react';

/** Inline error from the server or the network, with an optional retry. Announced to screen readers. */
export function ErrorNote({ message, onRetry }: { message: string | null; onRetry?: () => void }) {
  if (!message) return null;
  return (
    <div className="flex items-start gap-3 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800" role="alert">
      <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden />
      <p className="min-w-0 flex-1">{message}</p>
      {onRetry && (
        <button type="button" onClick={onRetry} className="inline-flex items-center gap-1 font-semibold text-rose-800 underline-offset-2 hover:underline">
          <RotateCw className="h-3.5 w-3.5" aria-hidden />
          Retry
        </button>
      )}
    </div>
  );
}
