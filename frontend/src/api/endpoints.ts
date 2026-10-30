import { api } from './client';
import type {
  AuthResponse,
  CreateProjectRequest,
  ProjectCountsResponse,
  ProjectDetailResponse,
  ProjectListParams,
  ProjectPage,
  ProjectPageResponse,
  ProposalContent,
  ProposalDiff,
  ProposalVersionResponse,
  ReviewRequest,
  ReviewResponse,
  TimelineEntry,
  UserResponse,
} from './types';

export const login = (email: string, password: string) =>
  api.post<AuthResponse>('/auth/login', { email, password }).then((r) => r.data);

export const register = (name: string, email: string, password: string) =>
  api.post<AuthResponse>('/auth/register', { name, email, password }).then((r) => r.data);

export const listFaculty = () =>
  api.get<UserResponse[]>('/users', { params: { role: 'FACULTY' } }).then((r) => r.data);

export const listProjects = ({ status, ...rest }: ProjectListParams = {}) =>
  api
    .get<ProjectPageResponse>('/projects', { params: { ...rest, status: status?.length ? status.join(',') : undefined } })
    .then((r): ProjectPage => ({ items: r.data.content, total: r.data.totalElements, page: r.data.page, size: r.data.size, totalPages: r.data.totalPages }));

export const getProjectCounts = () => api.get<ProjectCountsResponse>('/projects/summary').then((r) => r.data);

export const getProject = (id: string) =>
  api.get<ProjectDetailResponse>(`/projects/${id}`).then((r) => r.data);

export const createProject = (body: CreateProjectRequest) =>
  api.post<ProjectDetailResponse>('/projects', body).then((r) => r.data);

export const updateDraft = (projectId: string, content: ProposalContent) =>
  api.put<ProposalVersionResponse>(`/projects/${projectId}/proposal`, content).then((r) => r.data);

export const createNextVersion = (projectId: string) =>
  api.post<ProposalVersionResponse>(`/projects/${projectId}/proposals`).then((r) => r.data);

export const listVersions = (projectId: string) =>
  api.get<ProposalVersionResponse[]>(`/projects/${projectId}/proposals`).then((r) => r.data);

export const compareVersions = (projectId: string, from: number, to: number) =>
  api.get<ProposalDiff>(`/projects/${projectId}/proposals/compare`, { params: { from, to } }).then((r) => r.data);

export const submitProject = (projectId: string) =>
  api.post<ProjectDetailResponse>(`/projects/${projectId}/submit`).then((r) => r.data);

export const assignReviewer = (projectId: string, facultyId: string) =>
  api.post<ProjectDetailResponse>(`/projects/${projectId}/assign`, { facultyId }).then((r) => r.data);

export const recordReview = (projectId: string, body: ReviewRequest) =>
  api.post<ReviewResponse>(`/projects/${projectId}/reviews`, body).then((r) => r.data);

export const listReviews = (projectId: string) =>
  api.get<ReviewResponse[]>(`/projects/${projectId}/reviews`).then((r) => r.data);

export const getHistory = (projectId: string) =>
  api.get<TimelineEntry[]>(`/projects/${projectId}/history`).then((r) => r.data);
