import type { AuditLogResponse, ReviewDecision } from '../api/types';
import { DECISION_LABEL, STATUS_LABEL } from '../components/labels';

function parseDetails(details: string | null): Record<string, unknown> {
  if (!details) return {};
  try {
    const parsed: unknown = JSON.parse(details);
    return parsed !== null && typeof parsed === 'object' ? (parsed as Record<string, unknown>) : {};
  } catch {
    return {};
  }
}

/** One readable line for an audit entry: the status change, then what its stored details say. */
export function describeAuditEntry(entry: AuditLogResponse, userName: (id: string) => string): string {
  const { title, versionNumber, decision, reviewerName, previousReviewerId, reviewerId } = parseDetails(entry.details);
  const parts: string[] = [];
  if (entry.fromStatus && entry.toStatus) parts.push(`${STATUS_LABEL[entry.fromStatus]} → ${STATUS_LABEL[entry.toStatus]}`);
  else if (entry.toStatus) parts.push(`Created as ${STATUS_LABEL[entry.toStatus]}`);
  if (typeof title === 'string') parts.push(`“${title}”`);
  if (versionNumber !== undefined) parts.push(`Version ${versionNumber}`);
  if (typeof decision === 'string' && decision in DECISION_LABEL) parts.push(DECISION_LABEL[decision as ReviewDecision]);
  if (typeof previousReviewerId === 'string' && typeof reviewerId === 'string') parts.push(`${userName(previousReviewerId)} → ${userName(reviewerId)}`);
  else if (typeof reviewerName === 'string') parts.push(`Reviewer: ${reviewerName}`);
  return parts.join(' · ');
}
