# Prastav: interview preparation

Paths below are relative to `backend/src/main/java/com/prastav/` unless they start with `backend/`, `frontend/` or `docs/`.

## Part A. Ten viva questions on the design

**1. Why a modular monolith and not microservices?**
One small team, one database and workflows that must be atomic (a status change and its audit row commit together). Microservices would turn that into distributed transactions for no benefit at this scale. The modules (`auth`, `project`, `proposal`, `review`, `workflow`, `audit`) already talk only through public services (`ProjectStore`, `ProposalService`, `UserService`, `WorkflowService`) and events, so a module could be split out later along the same seams.

**2. Why the State pattern instead of a status enum with a switch?**
Each status owns its rules in one small class (`DraftState`, `UnderReviewState`, ...). Every action in `ProjectState` is a default method that throws `IllegalTransitionException`, so anything not explicitly allowed is illegal. Adding a status adds a class instead of editing every switch (Open/Closed), and each state is unit-tested with `new DraftState()`. The same call `submit` behaves differently in `DraftState` (to SUBMITTED) and `RevisionRequestedState` (to UNDER_REVIEW): polymorphism instead of if/else. Two safety nets: `ProjectStateFactory` refuses to start if a status has no state, and the `switch` in `AllowedActionsResolver` is exhaustive, so the compiler flags a new `Action`.

**3. Walk me through what happens when a faculty member clicks Approve.**
`ReviewController.record` (gated by `@PreAuthorize("hasRole('FACULTY')")`) calls `ReviewService.recordReview`. It picks `ApproveDecisionHandler` from its `EnumMap`, loads the latest submitted version, rejects a stale `versionNumber`, then the handler calls `WorkflowService.approve`. The facade loads the project, asks `ProjectStateFactory` for the current state and calls `approve`. In `UnderReviewState`, `Guards.requireAssignedReviewer` runs, then `project.moveTo(APPROVED)`. The project is saved (version check), `ProjectStatusChangedEvent` is published and `AuditEventListener` writes STATUS_CHANGED. Back in `ReviewService`, the `Review` is saved against that exact version and `ReviewRecordedEvent` writes REVIEW_RECORDED. All in one transaction. See `docs/uml/png/07_seq_resubmit_approve.png`.

**4. Why are submitted versions immutable, and where is that enforced?**
A reviewer's decision must refer to exactly the text they read, and the audit trail must be trustworthy. It is enforced in three layers: `ProposalVersion.updateContent()` and `submit()` throw `ConflictException` once SUBMITTED; `ProposalService.updateDraft` first asks the state whether EDIT_DRAFT is allowed (409 otherwise); and the database trigger `trg_proposal_versions_immutable` rejects any UPDATE or DELETE on a submitted row, even from SQL. Changes after submission happen only by creating version N+1 through `ProposalVersionFactory.nextDraftFrom`.

**5. Why events for auditing, and why is the audit listener synchronous?**
The workflow should not know auditing exists (SRP), and a new side effect is a new listener, not an edit to `WorkflowService` (OCP). The listener is a plain `@EventListener`, so it runs inside the publisher's transaction: an audit row exists if and only if the change committed. The planned M2 `NotificationListener` will instead use `@TransactionalEventListener(AFTER_COMMIT)`, because a notification must never be sent for a change that rolled back. That difference is deliberate.

**6. How do you stop two people changing the same project at the same time?**
`Project` has `@Version Long version`. Hibernate adds `WHERE version = ?` to the update; if another transaction committed first, the update hits zero rows and Spring throws `OptimisticLockingFailureException`, which `GlobalExceptionHandler` turns into 409 `CONCURRENT_UPDATE`. If the second request arrives after the first committed, it simply loads the new status and the state refuses it with 409 `ILLEGAL_TRANSITION`. The partial unique index `ux_proposal_one_open_draft` also stops two concurrent "create revision" calls from producing two drafts.

**7. Where is authorization enforced, and why in two places?**
Layer 1 is the role, checked at the endpoint with `@PreAuthorize` (cheap, declarative, visible in the controller). Layer 2 is ownership and assignment, which a role cannot express: "this student owns this project", "this faculty member is the assigned reviewer". That lives in `ProjectAccessPolicy` (reads and draft edits) and in `Guards` inside the state classes (transitions). Doing it in the service and domain means it also protects any future caller that is not a controller.

**8. How does the UI know which buttons to show?**
`GET /api/projects/{id}` returns `allowedActions`, computed by `AllowedActionsResolver` from the current state's `availableActions()` filtered by who the caller is. The React detail page renders buttons only from that list and never reasons about statuses itself, so the UI cannot drift from the backend rules. The backend still re-validates every call, so hiding a button is a convenience, not a security measure.

**9. Isn't the Strategy pattern for three review decisions over-engineering?**
For exactly three fixed decisions a switch would work. The handler interface earns its place because each decision maps to a different workflow call with different inputs, `ReviewService` stays closed for modification when a decision is added (for example a conditional approval), and the same lookup-by-key structure is reused for M2 notification channels. The cost is three tiny classes. I would not add a strategy where there is no expected variation.

**10. How do you test the workflow without Spring or a database?**
State classes, entities and `Guards` are plain Java, so tests build real objects (`TestData.projectIn(UNDER_REVIEW, owner, reviewer)`) and call the state directly. Services get their collaborators through constructors, so `WorkflowServiceTest` and `ReviewServiceTest` use Mockito mocks for `ProjectStore`, `ProposalService` and `ApplicationEventPublisher` but real states via `TestData.stateFactory()`. `ProjectStore` is the seam that makes this easy. Only `ProjectControllerWebTest` starts a Spring slice (`@WebMvcTest`) to prove JWT, `@PreAuthorize` and error mapping.

## Part B. Exactly which files to change

### B1. New status `ON_HOLD` (coordinator pauses a project under review, then resumes it)

| File | Change |
|---|---|
| `workflow/ProjectStatus.java` | add `ON_HOLD` (not terminal) |
| `workflow/Action.java` | add `HOLD`, `RESUME` |
| `workflow/ProjectState.java` | add `default void hold(Project project, Actor actor, String reason)` and `default void resume(Project project, Actor actor)`, both throwing `IllegalTransitionException` |
| `workflow/OnHoldState.java` (new) | `@Component`; `status()` ON_HOLD; `availableActions()` = {RESUME}; `resume`: `Guards.requireRole(actor, COORDINATOR)`, `project.moveTo(UNDER_REVIEW)` |
| `workflow/UnderReviewState.java` | override `hold`: require COORDINATOR, `Guards.requireComments(reason, Action.HOLD)`, `moveTo(ON_HOLD)`; add HOLD to `availableActions()` |
| `workflow/WorkflowService.java` | add `hold(UUID, Actor, String)` and `resume(UUID, Actor)` using the private `transition(...)` |
| `workflow/AllowedActionsResolver.java` | add `case HOLD, RESUME -> actor.hasRole(Role.COORDINATOR);` (the compiler forces this because the switch is exhaustive) |
| `project/web/ProjectController.java` | `POST /api/projects/{id}/hold` and `/resume` with `@PreAuthorize("hasRole('COORDINATOR')")` |
| `backend/src/main/resources/db/migration/V2__on_hold_status.sql` (new) | `ALTER TABLE projects DROP CONSTRAINT projects_status_check;` then add it back including `'ON_HOLD'` |
| `backend/src/test/java/com/prastav/support/TestData.java` | add `new OnHoldState()` to `stateFactory()` (otherwise the factory fails fast, by design) |
| `backend/src/test/java/com/prastav/workflow/OnHoldStateTest.java` (new) | resume legal for coordinator; everything else illegal; UnderReviewState hold tests |
| `frontend/src/api/types.ts`, `components/labels.ts`, `components/StatusBadge` CSS in `styles.css`, `components/WorkflowRail.tsx`, `pages/ProjectDetailPage.tsx` | new status and action, badge colour, buttons |
| `docs/uml/04_project_state.puml`, `docs/diagrams.md`, `docs/LLD.md` section 5 | diagram and table |

Nothing in `AuditEventListener` changes: HOLD and RESUME fall into STATUS_CHANGED.

### B2. New role `REVIEW_COMMITTEE` (read-only oversight of all projects)

| File | Change |
|---|---|
| `auth/Role.java` | add `REVIEW_COMMITTEE` |
| `backend/src/main/resources/db/migration/V2__review_committee_role.sql` (new) | replace `users_role_check` to include the new value |
| `project/ProjectService.java` | `listProjects` switch: `case REVIEW_COMMITTEE -> projectRepository.findAllByOrderByUpdatedAtDesc();` (compiler forces a case) |
| `project/ProjectAccessPolicy.java` | `canView` returns true for `REVIEW_COMMITTEE` |
| `auth/AuthService.java` | keep it out of self-registration (same rule as COORDINATOR) |
| `auth/DemoDataSeeder.java` | seed one committee member |
| controllers | only endpoints the role may call change, e.g. `@PreAuthorize("hasAnyRole('COORDINATOR','REVIEW_COMMITTEE')")` on `UserController.list` if needed |
| `workflow/AllowedActionsResolver.java` | no change: the committee has no actions, so it gets an empty list |
| `frontend/src/api/types.ts`, `components/labels.ts` (`ROLE_LABEL`), `pages/ProjectsPage.tsx` (`HEADING`, `EMPTY`) | `Record<Role, ...>` makes TypeScript flag every place |
| tests | `ProjectServiceTest.listIsScopedByRole`, a policy test for `canView` |

### B3. Two reviewers must approve before a project is APPROVED

Design: a second strategy, `ApprovalPolicy`, decides whether an APPROVE decision completes the approval.

| File | Change |
|---|---|
| `backend/src/main/resources/db/migration/V2__project_reviewers.sql` (new) | table `project_reviewers(project_id, reviewer_id, PRIMARY KEY(...))`, migrate existing `reviewer_id` rows, keep or drop the column |
| `project/Project.java` | `@ManyToMany Set<User> reviewers`; `assignReviewer` adds; `isReviewedBy` checks the set; `getReviewers()` |
| `workflow/SubmittedState.java` | allow assignment until two reviewers are set; move to UNDER_REVIEW when the second is added (or accept a list in one call) |
| `workflow/ApprovalPolicy.java` (new interface) | `boolean isSatisfied(Project project, long approvalsOnLatestVersion)` |
| `workflow/TwoReviewerApprovalPolicy.java` (new `@Component`) | returns `approvalsOnLatestVersion >= 2` |
| `review/ReviewRepository.java` | `long countByProposalVersion_IdAndDecision(UUID versionId, ReviewDecision decision)` |
| `review/ApproveDecisionHandler.java` | inject `ApprovalPolicy` and `ReviewRepository`; call `workflowService.approve` only when `isSatisfied(project, existingApprovals + 1)`; otherwise run `Guards.requireAssignedReviewer` and let the review be recorded with status unchanged |
| `review/ReviewService.java` | reject a second review of the same version by the same reviewer (409) |
| `project/web/ProjectDetailResponse.java`, `ProjectMapper.java`, `ProjectSummaryResponse.java` | `List<UserSummary> reviewers` |
| `workflow/AllowedActionsResolver.java` | hide APPROVE for a reviewer who already approved this version |
| frontend `types.ts`, `ProjectDetailPage.tsx`, `AssignReviewerPanel.tsx` | show and assign two reviewers |
| tests | `ApproveDecisionHandlerTest` (first approval keeps UNDER_REVIEW, second approves), `SubmittedStateTest`, `ReviewServiceTest` |

### B4. Limit resubmissions to 3

Version 1 is the original submission, so versions 2, 3 and 4 are the three resubmissions; submitting version 5 must fail.

| File | Change |
|---|---|
| `workflow/Guards.java` | `public static final int MAX_RESUBMISSIONS = 3;` and `requireResubmissionAllowed(ProposalVersion draft)`: if `draft.getVersionNumber() - 1 > MAX_RESUBMISSIONS` throw `ConflictException("Resubmission limit of 3 reached")` |
| `workflow/RevisionRequestedState.java` | call `Guards.requireResubmissionAllowed(draft)` in `submit` before `draft.submit()` |
| `proposal/ProposalService.java` (optional, better UX) | refuse `createNextVersion` when the next number would exceed the limit, so the student is told before writing v5 |
| `workflow/AllowedActionsResolver.java` (optional) | hide CREATE_REVISION when the limit is reached |
| `backend/src/test/java/com/prastav/workflow/RevisionRequestedStateTest.java` | `fourthResubmissionIsRejected` (v5 throws, status stays REVISION_REQUESTED) and `thirdResubmissionIsAllowed` (v4 goes UNDER_REVIEW) |

### B5. How the planned M2 notification channel plugs in

1. `notification/Notification.java`, `NotificationType.java`, `NotificationRepository.java`, and migration `V2__notifications.sql` (columns in LLD section 9.6).
2. `notification/NotificationChannel.java`: `String name(); void send(Notification notification);`
3. `notification/InAppNotificationChannel.java` (`@Component`, saves the row) and `notification/EmailNotificationChannel.java` (`@Component`, logs only).
4. `notification/NotificationService.java`: constructor takes `List<NotificationChannel>`; `notify(...)` builds the `Notification` and calls `send` on every channel.
5. `notification/NotificationListener.java`: `@TransactionalEventListener(phase = AFTER_COMMIT)` on `ProjectStatusChangedEvent`; maps ASSIGN_REVIEWER to the reviewer, APPROVE/REQUEST_REVISION/REJECT to the owner, resubmission to the reviewer.
6. `notification/web/NotificationController.java` + `NotificationResponse.java`: `GET /api/notifications`, `PATCH /api/notifications/{id}/read`.
7. Frontend: notification bell in `components/Layout.tsx`, polling `GET /api/notifications` every 30 seconds with TanStack Query.

**No existing workflow, project, proposal or review class changes.** A future channel (for example SMS) is one more `@Component` implementing `NotificationChannel`; Spring injects it into the list automatically. That is the live demonstration of Open/Closed for M2.

## Part C. Two-minute code walk for the demo

1. Open `workflow/ProjectState.java`: default methods throw.
2. Open `workflow/UnderReviewState.java`: three overrides, each two guard calls and one `moveTo`.
3. Open `workflow/WorkflowService.java`: the private `transition` template.
4. Open `backend/src/test/java/com/prastav/workflow/SubmittedStateTest.java`: `submitAndReviewActionsAreIllegalWhenSubmitted` is the PRD's own example of an illegal transition.
5. In Swagger, call `POST /api/projects/{id}/reviews` with APPROVE on a SUBMITTED project and show the 409 body.
