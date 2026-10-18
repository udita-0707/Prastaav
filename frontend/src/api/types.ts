// Mirrors the backend DTOs (com.prastav.*.web). Keep in sync with docs/LLD.md section 9.

export type Role = 'STUDENT' | 'FACULTY' | 'COORDINATOR';

export type ProjectStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'UNDER_REVIEW'
  | 'REVISION_REQUESTED'
  | 'APPROVED'
  | 'REJECTED';

export type Action =
  | 'SUBMIT'
  | 'ASSIGN_REVIEWER'
  | 'APPROVE'
  | 'REQUEST_REVISION'
  | 'REJECT'
  | 'EDIT_DRAFT'
  | 'CREATE_REVISION';

export type ReviewDecision = 'APPROVE' | 'REQUEST_REVISION' | 'REJECT';

export interface UserResponse {
  id: string;
  name: string;
  email: string;
  role: Role;
}

export interface UserSummary {
  id: string;
  name: string;
}

export interface AuthResponse {
  token: string;
  user: UserResponse;
}

export interface ProposalContent {
  problemStatement: string | null;
  objectives: string | null;
  methodology: string | null;
  expectedOutcome: string | null;
  timeline: string | null;
  referenceLink: string | null;
}

export interface ProposalVersionResponse extends ProposalContent {
  id: string;
  versionNumber: number;
  state: 'DRAFT' | 'SUBMITTED';
  missingFields: string[];
  submittedAt: string | null;
  createdAt: string;
}

export interface ProjectSummaryResponse {
  id: string;
  title: string;
  status: ProjectStatus;
  owner: UserSummary;
  reviewer: UserSummary | null;
  updatedAt: string;
}

export interface ProjectDetailResponse {
  id: string;
  title: string;
  description: string;
  techStack: string | null;
  teamMembers: string | null;
  status: ProjectStatus;
  owner: UserSummary;
  reviewer: UserSummary | null;
  currentProposal: ProposalVersionResponse | null;
  latestSubmittedVersion: number | null;
  allowedActions: Action[];
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProjectRequest {
  title: string;
  description: string;
  techStack: string;
  teamMembers: string;
  proposal: ProposalContent;
}

export interface ReviewRequest {
  decision: ReviewDecision;
  comments: string;
  versionNumber: number | null;
}

export interface ReviewResponse {
  id: string;
  versionNumber: number;
  decision: ReviewDecision;
  comments: string | null;
  reviewer: UserSummary;
  createdAt: string;
}

export interface ApiError {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  fieldErrors: { field: string; message: string }[];
}
