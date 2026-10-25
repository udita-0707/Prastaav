import { ExternalLink } from 'lucide-react';
import type { ProposalContent } from '../api/types';
import { Input, Textarea } from './ui';
import { FIELD_LABEL } from './labels';

type ProposalKey = keyof ProposalContent;

/** Limits mirror the backend's ProposalContent validation. */
export const PROPOSAL_MAX: Record<ProposalKey, number> = {
  problemStatement: 4000,
  objectives: 4000,
  methodology: 4000,
  expectedOutcome: 4000,
  timeline: 2000,
  referenceLink: 500,
};

/** Every field except the reference link is needed before a version can be submitted. */
export const REQUIRED_TO_SUBMIT: ProposalKey[] = ['problemStatement', 'objectives', 'methodology', 'expectedOutcome', 'timeline'];

const PLACEHOLDER: Record<ProposalKey, string> = {
  problemStatement: 'What problem does the project solve, and for whom?',
  objectives: 'What will the project achieve? List measurable objectives.',
  methodology: 'How will you build it: approach, architecture, tools and process.',
  expectedOutcome: 'What will exist at the end, and how will you know it works?',
  timeline: 'Main phases with their weeks, for example "Weeks 1-2: research".',
  referenceLink: 'https://',
};

export const EMPTY_PROPOSAL: ProposalContent = {
  problemStatement: '',
  objectives: '',
  methodology: '',
  expectedOutcome: '',
  timeline: '',
  referenceLink: '',
};

/** Editable subset of the proposal. Fields may be blank while drafting; required ones are marked. */
export function ProposalFields({
  value,
  onChange,
  fields,
  errors = {},
}: {
  value: ProposalContent;
  onChange: (next: ProposalContent) => void;
  fields: ProposalKey[];
  errors?: Partial<Record<ProposalKey, string>>;
}) {
  const set = (key: ProposalKey, text: string) => onChange({ ...value, [key]: text });
  return (
    <div className="space-y-6">
      {fields.map((key) =>
        key === 'referenceLink' ? (
          <Input
            key={key}
            label={FIELD_LABEL[key]}
            type="url"
            value={value[key] ?? ''}
            onChange={(e) => set(key, e.target.value)}
            placeholder={PLACEHOLDER[key]}
            hint="Optional: a repository, paper or document that supports the proposal."
            maxLength={PROPOSAL_MAX[key]}
            error={errors[key]}
            icon={<ExternalLink className="h-4 w-4" />}
          />
        ) : (
          <Textarea
            key={key}
            label={FIELD_LABEL[key]}
            value={value[key] ?? ''}
            onChange={(e) => set(key, e.target.value)}
            placeholder={PLACEHOLDER[key]}
            rows={key === 'timeline' ? 3 : 5}
            maxLength={PROPOSAL_MAX[key]}
            showCount
            required={REQUIRED_TO_SUBMIT.includes(key)}
            error={errors[key]}
          />
        ),
      )}
    </div>
  );
}

/** Read-only proposal display, used on the project page and in the wizard's review step. */
export function ProposalView({ value }: { value: ProposalContent }) {
  return (
    <dl className="divide-y divide-line/80">
      {REQUIRED_TO_SUBMIT.map((key) => (
        <div key={key} className="grid gap-1 py-4 first:pt-0 sm:grid-cols-[12rem_1fr] sm:gap-6">
          <dt className="text-sm font-semibold text-ink">{FIELD_LABEL[key]}</dt>
          <dd className="whitespace-pre-line text-sm leading-relaxed text-slate-700">
            {value[key]?.trim() ? value[key] : <span className="italic text-muted">Not written yet</span>}
          </dd>
        </div>
      ))}
      {value.referenceLink && (
        <div className="grid gap-1 py-4 sm:grid-cols-[12rem_1fr] sm:gap-6">
          <dt className="text-sm font-semibold text-ink">{FIELD_LABEL.referenceLink}</dt>
          <dd className="min-w-0 text-sm">
            <a href={value.referenceLink} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 break-all font-medium text-accent-700 hover:underline">
              {value.referenceLink}
              <ExternalLink className="h-3.5 w-3.5 shrink-0" aria-hidden />
            </a>
          </dd>
        </div>
      )}
    </dl>
  );
}
