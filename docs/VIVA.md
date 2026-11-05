# Prastav: viva sheet

Answers come only from this codebase. Class paths are relative to `backend/src/main/java/com/prastav/` unless they start with `backend/`, `frontend/` or `docs/`. "Section" means a section of `docs/LLD.md`.

## Part A. Design and patterns

### Workflow and patterns

**1. Why the State pattern and not an enum with a switch?**
Each status owns its rules in one class, such as `DraftState` or `UnderReviewState`. Every action in `ProjectState` is a default method that throws `IllegalTransitionException`, so an action is illegal unless a state allows it. With a switch, every action needs a case per status, and a forgotten case can let something through. The cost is more classes, so reading the flow means opening several files; the status table and diagrams offset that. *`workflow/ProjectState.java`, section 2.3.*

**2. How does the same call behave differently per status?**
`submit` on `DraftState` freezes the draft and moves to SUBMITTED. On `RevisionRequestedState` it also checks a reviewer is set and returns to UNDER_REVIEW. Every other state inherits the default and throws. `WorkflowService` makes the same call without an `if` on the status. *Section 3.*

**3. What does the Factory do here?**
`ProjectStateFactory` receives every `ProjectState` bean and maps status to state. Its constructor fails at startup if a status has no state or two, so the mistake cannot reach a request. `ProposalVersionFactory` is the only place that creates versions: v1, and vN+1 prefilled from vN, so numbering cannot be bypassed. *`workflow/ProjectStateFactory.java`, `proposal/ProposalVersionFactory.java`, section 5.*

**4. What is the Facade, and what does it protect?**
`WorkflowService` is the single entry point for every status and reviewer change. Controllers and other services never set a status themselves, so each change takes the same path in one transaction: load, run the state, save, publish. *`workflow/WorkflowService.java`, section 2.3.3.*

**5. Where is the Builder, and why there?**
`AuditLog.Builder`, reached through `AuditLog.builder()`. An entry has six fields and several are optional, so named steps read better than a long constructor. `AuditLog` has no setters, and its constructor requires `actorId` and `action`. *`audit/AuditLog.java`.*

**6. What does the Specification pattern give the lists?**
`ProjectSpecifications` and `AuditSpecifications` are small criteria combined with AND, and a missing value matches everything. That avoids one repository method per combination of filters. The filtering, ordering and paging all run in the database. *`project/ProjectSpecifications.java`, `audit/AuditSpecifications.java`, section 5.*

**7. Is anything a Singleton?**
Only by Spring's default scope: every `@Service` and `@Component` has one instance, and no class declares `@Scope` or a hand-written `getInstance`. One instance serves all requests, so these classes hold no request data. The state classes have no fields at all, and the caller travels as an `Actor` parameter. `RetryingNotificationChannel` is not a bean: `NotificationDispatcher` creates one per enabled channel at startup. *Section 5.*

### Strategy

**8. Where is Strategy used, and how does another university adopt the system?**
Three places. `ReviewDecisionHandler` has one implementation per decision. `NotificationChannel` has `InAppNotificationChannel` and `EmailNotificationChannel`, chosen by configuration. `EmailPolicy` has `PatternEmailPolicy`, which reads `prastav.institution`. Another university with the same address shape changes only configuration: the domain, name and pattern in `application.yml`. A different shape altogether is a new `EmailPolicy` class, and `AuthService` and `UserService` do not change. See the drill in Part B. *Sections 1.7 and 5.*

**9. Is a Strategy for three review decisions over-engineering?**
A switch would work for three fixed decisions. The handlers earn their place because each decision calls a different workflow method with different inputs, and `ReviewService` looks them up in a map, so it does not change when a decision is added. The cost is three very small classes. *`review/ReviewDecisionHandler.java`, section 2.5.*

### Events and notifications

**10. Why does audit run in the same transaction and notifications after commit?**
`AuditEventListener` is a plain `@EventListener`, so it runs inside the publisher's transaction: a change and its audit row commit or roll back together, and an audit failure undoes the change on purpose. `NotificationListener` is a `@TransactionalEventListener`, which runs only after commit, so nobody hears about a change that rolled back and a delivery problem cannot undo the change. *`audit/AuditEventListener.java`, `notification/NotificationListener.java`, section 2.4.*

**11. Why `REQUIRES_NEW` per attempt and `NOT_SUPPORTED` on the listener?**
`NotificationService.create` is `REQUIRES_NEW` and uses `saveAndFlush`, so a failing INSERT throws inside the retry loop, and each attempt starts clean. Before this, it joined the listener's transaction and did not flush: the INSERT ran at commit, after the retry had returned, so a failure was never retried and the row was lost. The listener is `NOT_SUPPORTED`, so it holds no transaction: one failure cannot mark a shared transaction rollback-only and take another recipient's notification with it, and no connection is held across a retry pause. *`notification/NotificationService.java`, section 2.4.1.*

**12. Why does the retry decorator rethrow, and why does the dispatcher log?**
Each has one job. `RetryingNotificationChannel` decides whether to try again, and after the last attempt rethrows the last failure, so it behaves like any `NotificationChannel`, whose contract is to signal failure by throwing. `NotificationDispatcher` owns the loop over channels, so it catches, logs at WARN in one place and moves on, which stops one failing channel affecting the others. *`notification/RetryingNotificationChannel.java`, `notification/NotificationDispatcher.java`.*

**13. What is the Hibernate connection-handling setting, and what failure did it fix?**
`hibernate.connection.handling_mode: DELAYED_ACQUISITION_AND_RELEASE_AFTER_TRANSACTION` in `application.yml` returns the connection at commit. Spring's default holds it while after-commit listeners run, and delivery needs a connection of its own. So a burst filled the pool with requests each holding one connection and waiting for a second. Measured with 30 simultaneous assignments: before, 10 succeeded and 20 failed with 500 after a 30 second timeout; after, all 30 succeeded in about 110 ms. The cost is that a transaction cannot set a custom isolation level, and none does. *Section 14.1.*

### Domain and layering

**14. Composition versus aggregation in the domain diagram?**
`Project *-- ProposalVersion` is composition: a version is created with and for one project, and `project_id` is NOT NULL. `Project o-- Review` and `Project o-- AuditLog` are aggregation: they are records about a project with their own meaning, and an audit entry's `projectId` is nullable. This is a modelling statement. JPA does not enforce it, since there are no cascades and no collections on `Project`, and the application never deletes these rows. *`docs/uml/02_domain_class.puml`.*

**15. What does `BaseEntity` give the entities?**
`User`, `Project`, `ProposalVersion`, `Review`, `AuditLog` and `Notification` inherit a UUID assigned in the constructor, a creation time, `equals` that compares ids, and `isNew` from `Persistable`. Because ids are assigned before saving, unit tests need no database, and `isNew` tells Spring Data to INSERT rather than merge. *`common/BaseEntity.java`, section 3.*

**16. Name one class for each SOLID letter.**
S: `ProposalVersionFactory` only creates and numbers versions. O: `NotificationDispatcher`, since a new channel is a new bean and a configuration entry. L: `RetryingNotificationChannel` honours the `NotificationChannel` contract, so the dispatcher treats it like the channel it wraps. I: `AuditRecorder` has one method. D: `AuthService` depends on the `EmailPolicy` interface and receives `PatternEmailPolicy` through its constructor. *Section 4.*

**17. Why do controllers hold no logic?**
A controller binds the request, checks the role with `@PreAuthorize`, calls one service and maps the result to a DTO. Rules live below it, so they also protect any other caller and can be tested without HTTP. Errors follow the same idea: controllers have no `try`/`catch`, and `GlobalExceptionHandler` produces the one `ApiError` body. *`project/web/ProjectController.java`, sections 2.1.1 and 9.1.*

**18. Why a modular monolith?**
One team, one database, and workflows that must be atomic: a status change and its audit row commit together. Microservices would turn that into distributed transactions for no gain at this size. Modules reach each other through public services such as `ProjectStore` and `UserService`, or through events, so a module could be split later along those seams. The cost is that the boundaries depend on discipline. *Sections 2.1 and 14.1.*

**19. Why are versions immutable, and where is that enforced?**
A review must refer to exactly the text the reviewer read. It is enforced three times: `ProposalVersion.updateContent` and `submit` throw `ConflictException` once SUBMITTED; `ProposalService.updateDraft` asks the state whether EDIT_DRAFT is allowed; and the trigger `trg_proposal_versions_immutable` rejects any UPDATE or DELETE of a submitted row, even from direct SQL. A change after submission is always a new version. *`proposal/ProposalVersion.java`, `backend/src/main/resources/db/migration/V1__init.sql`, section 8.3.*

### Concurrency and errors

**20. How does optimistic locking work, and why can a race give two different 409 codes?**
`Project` has `@Version Long version`, and the UPDATE matches only the version that was read. If two requests read the same version, the second UPDATE matches no row, and `GlobalExceptionHandler.handleConcurrentUpdate` returns 409 `CONCURRENT_UPDATE`. If the second request loads the project after the first has committed, it sees the new status and the state refuses it with 409 `ILLEGAL_TRANSITION`. Either way exactly one succeeds. The code depends only on timing. *Section 11.*

**21. What happens when a unique index rejects a write?**
A request can pass its checks and still lose to another one at commit. `GlobalExceptionHandler.handleIntegrityViolation` picks the answer from the constraint name: `uq_proposal_project_version` or `ux_proposal_one_open_draft` gives 409 `CONCURRENT_UPDATE`, and anything unknown gives 409 with a neutral message and a log entry. SQL and constraint names stay in the log and never reach the response. The database keeps the data correct, and the handler keeps the answer clean. *Section 9.1.*

**22. Why is a duplicate email 400 and not 409?**
Because the client should get the same answer whichever side of the race it lands on. `AuthService.register` and `UserService.createAccount` check first and throw `ValidationException` with `EMAIL_ALREADY_REGISTERED`, which is 400 `VALIDATION_FAILED`. When two requests both pass the check, `users_email_key` rejects the second, and the handler returns that same code and message. `ErrorCode` fixes each code's HTTP status in one place, so 409 would have needed a new code. *`common/GlobalExceptionHandler.java`, section 9.2.*

**23. Why is a cohort filter from a non-coordinator 403 and not ignored?**
`admissionYear` and `branch` are coordinator-only, and `ProjectService.listProjects` refuses them for anyone else. Ignoring them would return an unfiltered list that looks filtered, so the caller would believe a wrong answer; a 403 says what happened. `reviewerId` is different: it is allowed for everyone, because it is ANDed with the role scope and cannot widen what a caller sees. *`project/ProjectService.java`, `project/ProjectSpecifications.java`, section 7.7.*

**24. Where is authorization enforced?**
In two layers. `@PreAuthorize` checks the role at the controller. Ownership and assignment are checked below it, because a role cannot express them: `ProjectAccessPolicy` for reads and draft edits, and `Guards` inside the state classes for transitions. A faculty member who is not the assigned reviewer passes the first layer and is stopped by the second. *Section 13.*

### Limits

**25. What is not tested without a database, and what else is limited?**
`mvn test` uses no database, so JPQL, Flyway migrations, triggers, unique indexes and the real `@Version` column do not run there. `ConcurrentSubmissionWebTest` uses a store that imitates `@Version`, and `NotificationFlowTest` models when writes reach the database. A test checks the connection setting is present, but cannot detect pool starvation. The frontend has no automated tests. Known limits in the code: only `Project` has a version column, so simultaneous draft edits are last-write-wins, which is acceptable because a draft has one author. A draft edit overlapping a submit, inside a very short window, can give the edit a 500 or lose the edit; the web app saves and then submits in sequence, so it does not produce this by itself. A notification that fails every attempt is logged and not queued, and a crash between commit and dispatch loses it, because there is no outbox. *Sections 11, 12 and 14.2.*

## Part B. Live-coding drills

Paths follow the rule at the top of this file. New files are marked (new). The next migration number is `V7`. Always add a migration rather than editing an applied one, and update the docs and diagrams the change affects. `docs/INTERVIEW_PREP.md` Part B has more detail on the first five.

| Drill | Files to change | Watch out for |
|---|---|---|
| Add an ON_HOLD status | `workflow/ProjectStatus.java`, `workflow/Action.java` (`HOLD`, `RESUME`), `workflow/ProjectState.java` (default methods that throw), `workflow/OnHoldState.java` (new), `workflow/UnderReviewState.java`, `workflow/WorkflowService.java`, `workflow/AllowedActionsResolver.java`, `project/web/ProjectController.java`, `backend/src/main/resources/db/migration/V7__on_hold_status.sql` (new, replaces `projects_status_check`), `backend/src/test/java/com/prastav/support/TestData.java`, `backend/src/test/java/com/prastav/workflow/OnHoldStateTest.java` (new), `frontend/src/api/types.ts`, `frontend/src/components/labels.ts`, `frontend/src/components/ui/StatusPill.tsx`, `frontend/src/components/WorkflowRail.tsx`, `frontend/src/pages/ProjectDetailPage.tsx` | The factory fails at startup until the state exists, and tests fail until `TestData.stateFactory` has it. The switch in `AllowedActionsResolver` will not compile without the new actions. `AuditEventListener` needs no change: both fall into STATUS_CHANGED. |
| Add a role, such as REVIEW_COMMITTEE | `auth/Role.java`, `backend/src/main/resources/db/migration/V7__review_committee_role.sql` (new, replaces `users_role_check`), `project/ProjectSpecifications.java` (`visibleTo`), `project/ProjectService.java` (`countProjects`), `project/ProjectAccessPolicy.java` (`canView`), `auth/DemoDataSeeder.java`, the `@PreAuthorize` of each endpoint it may call, `frontend/src/api/types.ts`, `frontend/src/components/labels.ts`, `frontend/src/config/navigation.ts`, `frontend/src/config/demoAccounts.ts`, `frontend/src/pages/LoginPage.tsx`, `frontend/src/pages/HomePage.tsx`, `frontend/src/pages/ProjectsPage.tsx`, `frontend/src/router.tsx` | The compiler forces the two Java switches, and `Record<Role, ...>` makes TypeScript flag the frontend maps. `AuthService.register` already rejects every role but STUDENT. `UserService.createAccount` would accept the new role with no change. `AllowedActionsResolver` gives it no actions. |
| Add a notification channel, such as SMS | `notification/SmsNotificationChannel.java` (new `@Component` implementing `NotificationChannel`), `backend/src/main/resources/application.yml` or the `NOTIFICATION_CHANNELS` variable, `backend/src/test/java/com/prastav/notification/SmsNotificationChannelTest.java` (new) | No other class changes, and the retry wrapper applies automatically. `name()` must match the configured value, or `NotificationDispatcher` fails at startup. Throw on failure so the retry sees it. |
| Require two reviewers | `backend/src/main/resources/db/migration/V7__project_reviewers.sql` (new), `project/Project.java`, `workflow/SubmittedState.java`, `workflow/ApprovalPolicy.java` (new), `workflow/TwoReviewerApprovalPolicy.java` (new), `review/ReviewRepository.java`, `review/ApproveDecisionHandler.java`, `review/ReviewService.java`, `workflow/AllowedActionsResolver.java`, `project/web/ProjectDetailResponse.java`, `project/web/ProjectSummaryResponse.java`, `project/web/ProjectMapper.java`, `project/ProjectSpecifications.java`, `project/ProjectRepository.java` (`reviewerWorkload` and the reviewer counts), `notification/NotificationListener.java`, `workflow/WorkflowService.java` (`reassignReviewer`), `workflow/UnderReviewState.java`, `frontend/src/api/types.ts`, `frontend/src/pages/ProjectDetailPage.tsx`, `frontend/src/components/AssignReviewerPanel.tsx` | The largest drill: `reviewer` becomes a set, so every query and DTO using it changes. A second review of the same version by the same reviewer must be refused. Say this is a design change rather than start typing. |
| Limit resubmissions to three | `workflow/Guards.java` (`MAX_RESUBMISSIONS`, `requireResubmissionAllowed`), `workflow/RevisionRequestedState.java`, `proposal/ProposalService.java` (refuse `createNextVersion` early, optional), `backend/src/test/java/com/prastav/workflow/RevisionRequestedStateTest.java` | Version 1 is the original, so versions 2, 3 and 4 are the three resubmissions, and submitting version 5 fails with `ConflictException`. Check before `draft.submit()`. No schema change. |
| Add an email pattern for another university | `backend/src/main/resources/application.yml` (`student-email-pattern`) with `INSTITUTION_NAME` and `INSTITUTION_EMAIL_DOMAIN`, `VITE_INSTITUTION_NAME` and `VITE_INSTITUTION_EMAIL_DOMAIN` from `frontend/.env.example`, `frontend/src/pages/RegisterPage.tsx` (the placeholder and hint), `frontend/src/config/demoAccounts.ts`, `auth/DemoDataSeeder.java`, `backend/src/test/java/com/prastav/auth/InstitutionConfigTest.java`, `backend/src/test/java/com/prastav/auth/PatternEmailPolicyTest.java` | The pattern must keep the named groups `year` and `branch`. `PatternEmailPolicy` adds 2000 to a two-digit year. `DemoDataSeeder` calls `requireStudent` on the demo emails, so a different pattern makes the seed fail at startup unless those emails change or `SEED_DEMO_DATA=false`. If an address carries no year and branch, write a new `EmailPolicy` instead, and it must replace `PatternEmailPolicy` as the bean. |
| Add a filter to the project list, such as `techStack` | `project/ProjectListQuery.java` (the record and `of`), `project/ProjectSpecifications.java` (a new criterion in `forQuery`), `project/web/ProjectController.java` (`@RequestParam`), `backend/src/main/resources/db/migration/V7__project_tech_stack_index.sql` (new, if exact match), `frontend/src/api/types.ts` (`ProjectListParams`), `frontend/src/lib/useProjectListParams.ts`, `frontend/src/pages/ProjectsPage.tsx`, `backend/src/test/java/com/prastav/project/ProjectListQueryTest.java`, `backend/src/test/java/com/prastav/project/ProjectServiceTest.java`, `backend/src/test/java/com/prastav/project/ProjectSpecificationsTest.java`, `backend/src/test/java/com/prastav/project/web/ProjectControllerWebTest.java` | `ProjectListQuery.of` has positional parameters, so `ProjectListQueryTest`, `ProjectServiceTest` and `ProjectSpecificationsTest` stop compiling until updated. Ignore a null value in the criterion, as the others do. Decide whether it is coordinator-only; if so, add it to `filtersByCohort`, which gives other roles a 403. |
| Add a field to the proposal, such as `budget` | `backend/src/main/resources/db/migration/V7__proposal_budget.sql` (new), `proposal/ProposalContent.java`, `proposal/ProposalVersion.java` (field, `apply`, `content`, getter, and `missingFields` if required), `proposal/ProposalDiffService.java`, `proposal/web/ProposalVersionResponse.java`, `frontend/src/api/types.ts`, `frontend/src/components/labels.ts` (`FIELD_LABEL`), `frontend/src/components/ProposalFields.tsx`, `frontend/src/pages/ProjectWizardPage.tsx`, `backend/src/test/java/com/prastav/support/TestData.java`, `backend/src/test/java/com/prastav/proposal/ProposalDiffServiceTest.java`, `backend/src/test/java/com/prastav/proposal/ProposalServiceTest.java`, `backend/src/test/java/com/prastav/proposal/ProposalVersionTest.java` | `ProposalContent` is a record, so every `new ProposalContent` stops compiling until updated; that is the point. Add the field to `ProposalDiffService.compare`, or the Compare tab silently omits it. `nextDraftFrom` copies it through `content()`, so the factory needs no change. If required, only the submit check changes, since drafts may stay incomplete. |

## Part C. Before the viva

```bash
cd backend && mvn -q test
cd frontend && npm run build
```

For the code walk, `docs/INTERVIEW_PREP.md` Part C lists five files and a Swagger call in order. The demo accounts and the three commands that start the application are in `README.md`.
