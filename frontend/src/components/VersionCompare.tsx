import { useQuery } from '@tanstack/react-query';
import clsx from 'clsx';
import { ArrowRight } from 'lucide-react';
import { useState } from 'react';
import { errorMessage } from '../api/client';
import { compareVersions } from '../api/endpoints';
import type { FieldDiff, ProposalVersionResponse } from '../api/types';
import { ErrorNote } from './ErrorNote';
import { FIELD_LABEL } from './labels';
import { EmptyState, Select, Skeleton } from './ui';

function versionLabel(v: ProposalVersionResponse) {
  return `v${v.versionNumber} · ${v.state === 'DRAFT' ? 'draft' : 'submitted'}`;
}

function Value({ text, tone }: { text: string | null; tone: 'removed' | 'added' | 'same' }) {
  return (
    <div
      className={clsx(
        'min-h-[3rem] whitespace-pre-line break-words rounded-xl p-3 text-sm leading-relaxed ring-1 ring-inset',
        tone === 'removed' && 'bg-rose-50 text-rose-900 ring-rose-200',
        tone === 'added' && 'bg-green-50 text-green-900 ring-green-200',
        tone === 'same' && 'bg-slate-50 text-slate-700 ring-line',
      )}
    >
      {text || <span className="text-muted">—</span>}
    </div>
  );
}

function FieldRow({ field, from, to }: { field: FieldDiff; from: number; to: number }) {
  return (
    <li className="space-y-2">
      <div className="flex items-center gap-2">
        <h3 className="text-sm font-semibold text-ink">{FIELD_LABEL[field.name]}</h3>
        {field.changed ? (
          <span className="rounded-full bg-amber-50 px-2 py-0.5 text-xs font-semibold text-amber-800">Changed</span>
        ) : (
          <span className="text-xs text-muted">Unchanged</span>
        )}
      </div>
      <div className="grid gap-3 sm:grid-cols-2">
        <div>
          <p className="mb-1 text-xs font-semibold text-muted sm:hidden">v{from}</p>
          <Value text={field.fromValue} tone={field.changed ? 'removed' : 'same'} />
        </div>
        <div>
          <p className="mb-1 text-xs font-semibold text-muted sm:hidden">v{to}</p>
          <Value text={field.toValue} tone={field.changed ? 'added' : 'same'} />
        </div>
      </div>
    </li>
  );
}

/** Pick two proposal versions and see every field side by side, with changed fields highlighted. */
export function VersionCompare({ projectId, versions }: { projectId: string; versions: ProposalVersionResponse[] }) {
  const [from, setFrom] = useState(versions.at(-2)?.versionNumber ?? 0);
  const [to, setTo] = useState(versions.at(-1)?.versionNumber ?? 0);
  const ready = versions.length >= 2 && from !== to;
  const diff = useQuery({ queryKey: ['compare', projectId, from, to], queryFn: () => compareVersions(projectId, from, to), enabled: ready });

  if (versions.length < 2) {
    return <EmptyState title="Only one version so far" description="Once the student starts a second version, you can compare any two versions here." />;
  }
  const changed = diff.data?.fields.filter((f) => f.changed).length ?? 0;

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
        <Select label="From" value={from} onChange={(e) => setFrom(Number(e.target.value))} wrapperClassName="sm:w-48">
          {versions.map((v) => (
            <option key={v.id} value={v.versionNumber}>
              {versionLabel(v)}
            </option>
          ))}
        </Select>
        <ArrowRight className="hidden h-4 w-4 shrink-0 text-slate-400 sm:mb-3 sm:block" aria-hidden />
        <Select label="To" value={to} onChange={(e) => setTo(Number(e.target.value))} wrapperClassName="sm:w-48">
          {versions.map((v) => (
            <option key={v.id} value={v.versionNumber}>
              {versionLabel(v)}
            </option>
          ))}
        </Select>
      </div>

      {from === to && <p className="text-sm text-muted">Pick two different versions to compare.</p>}
      {diff.isLoading && <Skeleton className="h-64 w-full" />}
      <ErrorNote message={diff.error ? errorMessage(diff.error) : null} onRetry={() => diff.refetch()} />
      {ready && diff.data && (
        <>
          <p className="text-sm text-slate-700" aria-live="polite">
            {changed === 0 ? 'No field changed between these versions.' : `${changed} of ${diff.data.fields.length} fields changed.`}
          </p>
          <div className="hidden grid-cols-2 gap-3 text-xs font-semibold uppercase tracking-wide text-muted sm:grid">
            <span>Version {diff.data.fromVersion}</span>
            <span>Version {diff.data.toVersion}</span>
          </div>
          <ol className="space-y-6">
            {diff.data.fields.map((field) => (
              <FieldRow key={field.name} field={field} from={diff.data.fromVersion} to={diff.data.toVersion} />
            ))}
          </ol>
        </>
      )}
    </div>
  );
}
