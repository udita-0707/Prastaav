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
  | 'REASSIGN_REVIEWER'
  | 'EDIT_DRAFT'
  | 'CREATE_REVISION';

export type ReviewDecision = 'APPROVE' | 'REQUEST_REVISION' | 'REJECT';

export interface UserResponse {
  id: string;
  name: string;
  email: string;
  role: Role;
  /** Students only. */
  admissionYear: number | null;
  branch: string | null;
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
  description: string;
  teamMembers: string | null;
  status: ProjectStatus;
  owner: UserSummary;
  reviewer: UserSummary | null;
  /** First submission of the proposal; null while never submitted. */
  submittedAt: string | null;
  updatedAt: string;
}

export type ProjectSort = 'LATEST' | 'OLDEST' | 'TITLE';

/** Query parameters of GET /api/projects. All optional; page defaults to 0 and size to 10 (at most 50). */
export interface ProjectListParams {
  status?: ProjectStatus[];
  q?: string;
  reviewerId?: string;
  /** Coordinator only: the project owner's admission year, e.g. 2023. */
  admissionYear?: number;
  /** Coordinator only: the project owner's branch, e.g. CSAI. */
  branch?: string;
  sort?: ProjectSort;
  page?: number;
  size?: number;
}

/** Body of GET /api/projects: one page of rows plus paging numbers. */
export interface ProjectPageResponse {
  content: ProjectSummaryResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ProjectPage {
  items: ProjectSummaryResponse[];
  /** Matches across all pages. */
  total: number;
  /** Zero-based page index the server returned. */
  page: number;
  size: number;
  totalPages: number;
}

export interface ProjectCountsResponse {
  total: number;
  byStatus: Record<ProjectStatus, number>;
  awaitingAssignment: number | null;
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

export type TimelineEntryType = 'VERSION_CREATED' | 'STATUS_CHANGED' | 'REVIEW';

export interface TimelineEntry {
  at: string;
  type: TimelineEntryType;
  actorId: string;
  actorName: string | null;
  summary: string;
  versionNumber: number | null;
}
