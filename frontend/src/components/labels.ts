import type { AuditAction, ProjectStatus, ProposalContent, ReviewDecision, Role, TimelineEntryType } from '../api/types';

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
  REQUEST_REVISION: 'Changes requested',
  REJECT: 'Rejected',
};

export const TIMELINE_TYPE_LABEL: Record<TimelineEntryType, string> = {
  VERSION_CREATED: 'Version',
  STATUS_CHANGED: 'Status change',
  REVIEW: 'Review',
};

export const AUDIT_ACTION_LABEL: Record<AuditAction, string> = {
  PROJECT_CREATED: 'Project created',
  VERSION_CREATED: 'Version created',
  DRAFT_UPDATED: 'Draft updated',
  PROPOSAL_SUBMITTED: 'Proposal submitted',
  REVIEWER_ASSIGNED: 'Reviewer assigned',
  REVIEWER_REASSIGNED: 'Reviewer reassigned',
  REVIEW_RECORDED: 'Review recorded',
  STATUS_CHANGED: 'Status changed',
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
