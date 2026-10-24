# Prastav M1: commit plan (two people)

**Person A**: workflow, project, proposal, review, audit, backend tests, LLD class and sequence diagrams.
**Person B**: setup, Flyway, auth/security, seed data, Swagger, frontend, README, ER / use-case / architecture diagrams.

## Rules

- Conventional commits: `type(scope): summary` with types `feat`, `test`, `docs`, `chore`, `build`.
- Every commit builds on its own: `cd backend && mvn -q test` passes, and once the frontend exists `cd frontend && npm run build` passes.
- Small commits; tests travel with the code they test.
- Branches: `main` (stable) and `develop`; each commit below is made on a short feature branch and merged into `develop` through a PR reviewed by the other person. `develop` is merged into `main` before the evaluation.
- Diagrams and the LLD are committed in `docs/` next to the code they describe, in the same PR.
- B's setup and auth commits (B1 to B6) land before A starts, because every A module uses `BaseEntity`, the exceptions, `User` and `Actor`.

## Order of work

The **#** column is the global order in which PRs are merged into `develop`. Within a person the numbering (A1, B1, ...) is their own sequence.

| # | Who | Commit message | Files | Builds because |
|---|---|---|---|---|
| 1 | B1 | `chore(backend): scaffold Spring Boot 3 project with Maven` | `backend/pom.xml`, `PrastavApplication.java`, `application.yml`, `.gitignore` | no tests yet; `mvn -q test` compiles |
| 2 | B2 | `chore(db): add PostgreSQL 16 docker-compose service` | `docker-compose.yml` | config only |
| 3 | B3 | `feat(common): add ApiError, error codes, exception hierarchy and BaseEntity` | `common/ApiError`, `FieldErrorDetail`, `ErrorCode`, `PrastavException`, `NotFoundException`, `ForbiddenException`, `ValidationException`, `ConflictException`, `UnauthenticatedException`, `BaseEntity`, `GlobalExceptionHandler` | only Spring and JPA imports |
| 4 | B4 | `feat(db): add Flyway V1 schema with constraints and immutability triggers` | `db/migration/V1__init.sql` | SQL only |
| 5 | B5 | `feat(auth): add User, Role, Actor and UserService` | `auth/Role`, `User`, `Actor`, `AuthResult`, `UserRepository`, `UserService` | uses common from #3 |
| 6 | B6 | `feat(auth): add JWT authentication, security config and auth endpoints` | `auth/JwtService`, `JwtAuthenticationFilter`, `RestAuthErrorHandler`, `SecurityConfig`, `AuthService`, `auth/web/*` | uses #5 |
| 7 | B7 | `feat(auth): seed demo coordinator, faculty and students` | `auth/DemoDataSeeder` | uses #5, #6 (`PasswordEncoder`) |
| 8 | B8 | `docs(api): configure Swagger UI with bearer authentication` | `common/OpenApiConfig` | annotations only |
| 9 | A1 | `feat(workflow): add ProjectStatus, Action and IllegalTransitionException` | `workflow/ProjectStatus`, `Action`, `IllegalTransitionException` | uses common |
| 10 | A2 | `feat(project): add Project entity with optimistic locking, ProjectStore and access policy` | `project/Project`, `ProjectRepository`, `ProjectStore`, `ProjectAccessPolicy` | uses #5, #9 |
| 11 | A3 | `feat(proposal): add immutable ProposalVersion and ProposalVersionFactory` | `proposal/ProposalVersion`, `ProposalVersionState`, `ProposalContent`, `ProposalVersionFactory`, `ProposalVersionRepository`; tests `ProposalVersionTest`, `support/TestData` (users, projects, versions only) | uses #10 |
| 12 | A4 | `feat(workflow): implement State pattern with six states and ProjectStateFactory` | `workflow/ProjectState`, `Guards`, `DraftState`, `SubmittedState`, `UnderReviewState`, `RevisionRequestedState`, `ApprovedState`, `RejectedState`, `ProjectStateFactory`; tests `DraftStateTest`, `SubmittedStateTest`, `UnderReviewStateTest`, `RevisionRequestedStateTest`, `TerminalStatesTest`, `ProjectStateFactoryTest`; `TestData.stateFactory()` | uses #10, #11 |
| 13 | A5 | `feat(proposal): add draft editing, revision versions and proposal endpoints` | `proposal/ProposalService`, `ProposalVersionCreatedEvent`, `ProposalDraftUpdatedEvent`, `proposal/web/*`; test `ProposalServiceTest` | uses `ProjectStateFactory` from #12 |
| 14 | A6 | `feat(workflow): add WorkflowService facade, status events and allowed actions` | `workflow/WorkflowService`, `ProjectStatusChangedEvent`, `AllowedActionsResolver`; tests `WorkflowServiceTest`, `AllowedActionsResolverTest` | uses #13 |
| 15 | B9 | `feat(frontend): scaffold Vite React app with API client, auth and login` | `frontend/package.json`, `package-lock.json`, `tsconfig.json`, `vite.config.ts`, `index.html`, `src/main.tsx`, `src/App.tsx` (login/register routes), `src/api/*`, `src/auth/*`, `src/pages/LoginPage.tsx`, `RegisterPage.tsx`, `src/components/Layout.tsx`, `ErrorNote.tsx`, `src/styles.css` | `npm run build` |
| 16 | A7 | `feat(project): add project creation, role-scoped list, detail and submit/assign endpoints` | `project/ProjectService`, `NewProject`, `ProjectView`, `ProjectCreatedEvent`, `project/web/*` (controller without `/history`); test `ProjectServiceTest` | uses #14 |
| 17 | A8 | `feat(review): add review decisions with strategy handlers` | `review/*`, `review/web/*`; test `ReviewServiceTest` | uses #14 |
| 18 | B10 | `feat(frontend): add project list, create project and draft editor pages` | `src/pages/ProjectsPage.tsx`, `CreateProjectPage.tsx`, `ProposalEditorPage.tsx`, `src/components/ProposalFields.tsx`, `StatusBadge.tsx`, `labels.ts` | `npm run build` |
| 19 | A9 | `feat(audit): record every domain event in an append-only audit log` | `audit/*`; test `AuditEventListenerTest` | listens to events from #13, #14, #16, #17 |
| 20 | A10 | `feat(project): add merged project history endpoint` | `project/ProjectHistoryService`, `TimelineEntry`, `TimelineEntryType`, `ProjectController` (`/history`) | uses #19 |
| 21 | B11 | `feat(frontend): add project detail with workflow rail, review and assignment panels` | `src/pages/ProjectDetailPage.tsx`, `src/components/WorkflowRail.tsx`, `ReviewPanel.tsx`, `AssignReviewerPanel.tsx`, routes in `App.tsx` | `npm run build` |
| 22 | A11 | `test(web): add MockMvc tests for 401, 403 and 409 error mapping` | `project/web/ProjectControllerWebTest` | uses #6 and #16 |
| 23 | B12 | `docs: add README with run steps, demo script and troubleshooting` | `README.md` | docs |
| 24 | B13 | `docs(uml): add use-case, ER and architecture diagrams` | `docs/uml/_style.iuml`, `01_use_case.puml`, `10_er.puml`, `11_component_architecture.puml`, their PNGs, matching sections of `docs/diagrams.md` | docs |
| 25 | A12 | `docs(uml): add domain, State-pattern, state, sequence and event diagrams` | `docs/uml/02_*`, `03_*`, `04_*`, `05_*` to `09_*`, `12_*` (.puml + PNG), the rest of `docs/diagrams.md`, `docs/uml/mermaid-png/` | docs |
| 26 | A13 | `docs(lld): add low level design for the whole system with M1/M2 tags` | `docs/LLD.md`, `docs/LLD.pdf` | docs |
| 27 | A14 | `docs: add interview preparation and change guide` | `docs/INTERVIEW_PREP.md`, `COMMIT_PLAN.md` | docs |

**Counts:** Person A 14 commits (A1 to A14), Person B 13 commits (B1 to B13).

## Notes per commit

- **#11** introduces `support/TestData` with only `student()`, `faculty()`, `coordinator()`, `actor()`, `completeContent()`, `project()`, `projectIn()`, `draft()`, `submittedVersion()`; **#12** adds `stateFactory()`.
- **#16** ships `ProjectController` without the `history` method; **#20** adds it with its constructor parameter `ProjectHistoryService`.
- **#19** must come after #17 because `AuditEventListener` imports `ReviewRecordedEvent`.
- **#22** needs `SecurityConfig`, `JwtService`, `RestAuthErrorHandler` from #6.
- Diagrams in #24 to #26 are generated from the final code; regenerate PNGs with `java -jar plantuml.jar -tpng -o png docs/uml/*.puml` if a name changes, and keep names identical to the code.

## Suggested PR review pairs

A reviews B's security and Flyway work (#4, #6) because the workflow depends on them; B reviews A's state machine (#12, #14) so both can explain the whole engine in the viva.
