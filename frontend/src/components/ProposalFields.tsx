import type { ProposalContent } from '../api/types';
import { FIELD_LABEL } from './labels';

const MULTILINE: (keyof ProposalContent)[] = [
  'problemStatement',
  'objectives',
  'methodology',
  'expectedOutcome',
  'timeline',
];

export const EMPTY_PROPOSAL: ProposalContent = {
  problemStatement: '',
  objectives: '',
  methodology: '',
  expectedOutcome: '',
  timeline: '',
  referenceLink: '',
};

/** Editable proposal form. Fields may be left blank while drafting; all but the link are needed to submit. */
export function ProposalFields({
  value,
  onChange,
}: {
  value: ProposalContent;
  onChange: (next: ProposalContent) => void;
}) {
  const set = (key: keyof ProposalContent, text: string) => onChange({ ...value, [key]: text });
  return (
    <div className="stack">
      {MULTILINE.map((key) => (
        <label key={key} className="field">
          <span>{FIELD_LABEL[key]}</span>
          <textarea rows={key === 'timeline' ? 2 : 4} value={value[key] ?? ''} onChange={(e) => set(key, e.target.value)} />
        </label>
      ))}
      <label className="field">
        <span>
          {FIELD_LABEL.referenceLink} <em className="hint">optional</em>
        </span>
        <input type="url" value={value.referenceLink ?? ''} onChange={(e) => set('referenceLink', e.target.value)} />
      </label>
    </div>
  );
}

/** Read-only proposal display. */
export function ProposalView({ value }: { value: ProposalContent }) {
  return (
    <dl className="proposal">
      {MULTILINE.map((key) => (
        <div key={key}>
          <dt>{FIELD_LABEL[key]}</dt>
          <dd>{value[key]?.trim() ? value[key] : <span className="muted">Not written yet</span>}</dd>
        </div>
      ))}
      {value.referenceLink && (
        <div>
          <dt>{FIELD_LABEL.referenceLink}</dt>
          <dd>
            <a href={value.referenceLink} target="_blank" rel="noreferrer">
              {value.referenceLink}
            </a>
          </dd>
        </div>
      )}
    </dl>
  );
}
