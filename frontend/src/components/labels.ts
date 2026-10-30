import type { ProjectStatus, ProposalContent, ReviewDecision, Role, TimelineEntryType } from '../api/types';

export const STATUS_LABEL: Record<ProjectStatus, string> = {
  DRAFT: 'Draft',
  SUBMITTED: 'Submitted',
  UNDER_REVIEW: 'Under Review',
  REVISION_REQUESTED: 'Changes Requested',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
};

export const DECISION_LABEL: Record<ReviewDecision, string> = {
  APPROVE: 'Approved',
  REQUEST_REVISION: 'Revision requested',
  REJECT: 'Rejected',
};

export const TIMELINE_TYPE_LABEL: Record<TimelineEntryType, string> = {
  VERSION_CREATED: 'Version',
  STATUS_CHANGED: 'Status change',
  REVIEW: 'Review',
};

export const ROLE_LABEL: Record<Role, string> = {
  STUDENT: 'Student',
  FACULTY: 'Faculty',
  COORDINATOR: 'Coordinator',
};

export const FIELD_LABEL: Record<keyof ProposalContent, string> = {
  problemStatement: 'Problem statement',
  objectives: 'Objectives',
  methodology: 'Methodology',
  expectedOutcome: 'Expected outcome',
  timeline: 'Timeline',
  referenceLink: 'Reference link',
};
