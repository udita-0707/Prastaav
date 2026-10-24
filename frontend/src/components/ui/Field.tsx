import clsx from 'clsx';
import {
  forwardRef,
  useId,
  type InputHTMLAttributes,
  type ReactNode,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from 'react';

interface FieldFrameProps {
  label?: ReactNode;
  hint?: ReactNode;
  error?: string | null;
  required?: boolean;
  counter?: { length: number; max: number };
  id: string;
  className?: string;
  children: ReactNode;
}

/** Label, control, then a row with the hint or error on the left and the character counter on the right. */
function FieldFrame({ label, hint, error, required, counter, id, className, children }: FieldFrameProps) {
  const showFooter = hint || error || counter;
  return (
    <div className={clsx('flex flex-col gap-1.5', className)}>
      {label && (
        <label htmlFor={id} className="text-sm font-semibold text-ink">
          {label}
          {required && (
            <span className="ml-0.5 text-rose-600" aria-hidden>
              *
            </span>
          )}
        </label>
      )}
      {children}
      {showFooter && (
        <div className="flex items-start justify-between gap-3 text-xs">
          {error ? (
            <p id={`${id}-error`} className="font-medium text-rose-700" role="alert">
              {error}
            </p>
          ) : hint ? (
            <p id={`${id}-hint`} className="text-muted">
              {hint}
            </p>
          ) : (
            <span />
          )}
          {counter && (
            <span className={clsx('tabular-nums', counter.length > counter.max ? 'text-rose-700' : 'text-muted')} aria-live="polite">
              {counter.length}/{counter.max}
            </span>
          )}
        </div>
      )}
    </div>
  );
}

const CONTROL =
  'w-full rounded-field border bg-white text-sm text-ink placeholder:text-slate-400 transition-shadow ' +
  'focus:border-brand-500 focus:outline-none focus:shadow-focus disabled:bg-slate-50 disabled:text-muted';

function describedBy(id: string, error?: string | null, hint?: ReactNode) {
  return error ? `${id}-error` : hint ? `${id}-hint` : undefined;
}

interface CommonProps {
  label?: ReactNode;
  hint?: ReactNode;
  error?: string | null;
  /** Shows a live "n/max" counter under the control. */
  showCount?: boolean;
  icon?: ReactNode;
  trailing?: ReactNode;
  wrapperClassName?: string;
}

export const Input = forwardRef<HTMLInputElement, InputHTMLAttributes<HTMLInputElement> & CommonProps>(function Input(
  { label, hint, error, showCount, icon, trailing, wrapperClassName, className, id, required, maxLength, value, ...rest },
  ref,
) {
  const autoId = useId();
  const inputId = id ?? autoId;
  const counter = showCount && maxLength ? { length: String(value ?? '').length, max: maxLength } : undefined;
  return (
    <FieldFrame label={label} hint={hint} error={error} required={required} counter={counter} id={inputId} className={wrapperClassName}>
      <div className="relative">
        {icon && <span className="pointer-events-none absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">{icon}</span>}
        <input
          ref={ref}
          id={inputId}
          required={required}
          maxLength={maxLength}
          value={value}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy(inputId, error, hint)}
          className={clsx(CONTROL, 'h-11', icon ? 'pl-10' : 'pl-3.5', trailing ? 'pr-11' : 'pr-3.5', error ? 'border-rose-400' : 'border-line', className)}
          {...rest}
        />
        {trailing && <span className="absolute right-2 top-1/2 -translate-y-1/2">{trailing}</span>}
      </div>
    </FieldFrame>
  );
});

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaHTMLAttributes<HTMLTextAreaElement> & CommonProps>(function Textarea(
  { label, hint, error, showCount, wrapperClassName, className, id, required, maxLength, value, rows = 4, ...rest },
  ref,
) {
  const autoId = useId();
  const inputId = id ?? autoId;
  const counter = showCount && maxLength ? { length: String(value ?? '').length, max: maxLength } : undefined;
  return (
    <FieldFrame label={label} hint={hint} error={error} required={required} counter={counter} id={inputId} className={wrapperClassName}>
      <textarea
        ref={ref}
        id={inputId}
        rows={rows}
        required={required}
        maxLength={maxLength}
        value={value}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy(inputId, error, hint)}
        className={clsx(CONTROL, 'resize-y px-3.5 py-3 leading-relaxed', error ? 'border-rose-400' : 'border-line', className)}
        {...rest}
      />
    </FieldFrame>
  );
});

export const Select = forwardRef<HTMLSelectElement, SelectHTMLAttributes<HTMLSelectElement> & CommonProps>(function Select(
  { label, hint, error, icon, wrapperClassName, className, id, required, children, ...rest },
  ref,
) {
  const autoId = useId();
  const inputId = id ?? autoId;
  return (
    <FieldFrame label={label} hint={hint} error={error} required={required} id={inputId} className={wrapperClassName}>
      <div className="relative">
        {icon && <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">{icon}</span>}
        <select
          ref={ref}
          id={inputId}
          required={required}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy(inputId, error, hint)}
          className={clsx(
            CONTROL,
            'h-11 appearance-none bg-[url("data:image/svg+xml;utf8,<svg xmlns=%27http://www.w3.org/2000/svg%27 width=%2716%27 height=%2716%27 fill=%27none%27 stroke=%27%2364748B%27 stroke-width=%272%27 viewBox=%270 0 24 24%27><path d=%27m6 9 6 6 6-6%27/></svg>")] bg-[length:16px] bg-[right_0.75rem_center] bg-no-repeat pr-9',
            icon ? 'pl-9' : 'pl-3.5',
            error ? 'border-rose-400' : 'border-line',
            className,
          )}
          {...rest}
        >
          {children}
        </select>
      </div>
    </FieldFrame>
  );
});
