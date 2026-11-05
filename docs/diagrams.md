# Prastav diagrams (Mermaid)

The same diagrams as the PlantUML sources in `docs/uml/*.puml` (rendered PNGs in `docs/uml/png/`).
Names match the code exactly, and every element exists in the code.
Mermaid renders of these blocks are in `docs/uml/mermaid-png/`.

| Section | PlantUML source and PNG name |
|---|---|
| 1. Use cases | `01_use_case` |
| 2. Domain class diagram | `02_domain_class` |
| 3. Workflow engine: State pattern | `03_workflow_state_pattern` |
| 4. Project state diagram with guards | `04_project_state` |
| 5. Sequence: create project and submit v1 | `05_seq_create_submit` |
| 6. Sequence: assign, review, request revision | `06_seq_assign_review_revision` |
| 7. Sequence: revise, resubmit, approve | `07_seq_resubmit_approve` |
| 8. Sequence: illegal transition rejected | `08_seq_illegal_transition` |
| 9. ER diagram | `10_er` |
| 10. Component / architecture | `11_component_architecture` |
| 11. Observer and Strategy | `12_events_and_strategies` |
| 12. Sequence: notification dispatch | `09_seq_notification_dispatch` |
| 13. Notification channels | `13_notification_classes` |
| 14. Auth view | `14_auth_classes` |
| 15. Activity: submit, assign, review, revise or approve | `15_activity_workflow` |

## 1. Use cases

```mermaid
flowchart LR
  Student([Student])
  Faculty([Faculty])
  Coordinator([Coordinator])
  UC1["Log in (JWT)"]
  UC2["Register with an institutional student email"]
  UC3["Create project with draft v1"]
  UC4["Edit own draft"]
  UC5["Submit / resubmit proposal"]
  UC6["Create revision vN+1"]
  UC7["View feedback and reviews"]
  UC8["View project timeline"]
  UC9["Compare two proposal versions"]
  UC10["Assign or reassign faculty reviewer"]
  UC11["Review latest submitted version<br/>(approve / request revision / reject)"]
  UC12["View all projects"]
  UC13["Filter, search and page project lists"]
  UC14["View dashboard counts by status"]
  UC15["Create faculty or coordinator account"]
  UC16["Read and mark own notifications"]
  UC17["View coordinator dashboard<br/>(awaiting assignment, reviewer workload)"]
  UC18["Query audit log<br/>(project, actor, action, date range)"]
  Student --> UC1 & UC2 & UC3 & UC4 & UC5 & UC6 & UC7 & UC8 & UC9 & UC13 & UC14 & UC16
  Faculty --> UC1 & UC7 & UC8 & UC9 & UC11 & UC13 & UC14 & UC16
  Coordinator --> UC1 & UC7 & UC8 & UC9 & UC10 & UC12 & UC13 & UC14 & UC15 & UC16 & UC17 & UC18
```

## 2. Domain class diagram

```mermaid
classDiagram
  class Persistable["Persistable<UUID>"] {
    <<interface>>
    +isNew() boolean
  }
  class BaseEntity {
    <<abstract>>
    -UUID id
    -Instant createdAt
    -boolean newEntity
    +getId() UUID
    +getCreatedAt() Instant
    +isNew() boolean
    +equals(Object other) boolean
    +hashCode() int
  }
  class User {
    -String name
    -String email
    -String passwordHash
    -Role role
    -Integer admissionYear
    -String branch
    +student(String name, StudentEmail studentEmail, String passwordHash)$ User
    +toActor() Actor
    +getRole() Role
  }
  class StudentEmail {
    <<record>>
    String email
    int admissionYear
    String branch
  }
  class Role {
    <<enumeration>>
    STUDENT
    FACULTY
    COORDINATOR
  }
  class Project {
    -String title
    -String description
    -String techStack
    -String teamMembers
    -User owner
    -User reviewer
    -ProjectStatus status
    -Long version
    -Instant updatedAt
    +moveTo(ProjectStatus next)
    +assignReviewer(User faculty)
    +isOwnedBy(UUID userId) boolean
    +isReviewedBy(UUID userId) boolean
    +getVersion() Long
  }
  class ProjectStatus {
    <<enumeration>>
    DRAFT
    SUBMITTED
    UNDER_REVIEW
    REVISION_REQUESTED
    APPROVED
    REJECTED
    +isTerminal() boolean
  }
  class ProposalVersion {
    -Project project
    -int versionNumber
    -String problemStatement
    -String objectives
    -String methodology
    -String expectedOutcome
    -String timeline
    -String referenceLink
    -ProposalVersionState state
    -Instant submittedAt
    -UUID createdBy
    +updateContent(ProposalContent content)
    +submit()
    +isDraft() boolean
    +missingFields() List~String~
    +isComplete() boolean
    +content() ProposalContent
  }
  class ProposalVersionState {
    <<enumeration>>
    DRAFT
    SUBMITTED
  }
  class ProposalContent {
    <<record>>
    String problemStatement
    String objectives
    String methodology
    String expectedOutcome
    String timeline
    String referenceLink
    +empty()$ ProposalContent
  }
  class ProposalDiff {
    <<record>>
    int fromVersion
    int toVersion
    List~FieldDiff~ fields
  }
  class FieldDiff {
    <<record>>
    String name
    String fromValue
    String toValue
    boolean changed
  }
  class ProposalDiffService {
    +compare(UUID projectId, int fromVersion, int toVersion, Actor actor) ProposalDiff
  }
  class Review {
    -Project project
    -ProposalVersion proposalVersion
    -User reviewer
    -ReviewDecision decision
    -String comments
    +getDecision() ReviewDecision
    +getProposalVersion() ProposalVersion
  }
  class ReviewDecision {
    <<enumeration>>
    APPROVE
    REQUEST_REVISION
    REJECT
  }
  class AuditLog {
    -UUID projectId
    -UUID actorId
    -AuditAction action
    -ProjectStatus fromStatus
    -ProjectStatus toStatus
    -String details
    +builder()$ AuditLog.Builder
  }
  class AuditLogBuilder["AuditLog.Builder"] {
    +projectId(UUID projectId) Builder
    +actorId(UUID actorId) Builder
    +action(AuditAction action) Builder
    +fromStatus(ProjectStatus fromStatus) Builder
    +toStatus(ProjectStatus toStatus) Builder
    +details(String details) Builder
    +build() AuditLog
  }
  class AuditAction {
    <<enumeration>>
    PROJECT_CREATED
    VERSION_CREATED
    DRAFT_UPDATED
    PROPOSAL_SUBMITTED
    REVIEWER_ASSIGNED
    REVIEWER_REASSIGNED
    REVIEW_RECORDED
    STATUS_CHANGED
  }
  class Notification {
    -User recipient
    -Project project
    -NotificationType type
    -String message
    -boolean read
    +markRead()
    +isAddressedTo(UUID userId) boolean
    +isRead() boolean
  }
  class NotificationType {
    <<enumeration>>
    REVIEWER_ASSIGNED
    REVIEWER_UNASSIGNED
    REVIEW_DECISION
    RESUBMITTED
  }
  Persistable <|.. BaseEntity
  BaseEntity <|-- User
  BaseEntity <|-- Project
  BaseEntity <|-- ProposalVersion
  BaseEntity <|-- Review
  BaseEntity <|-- AuditLog
  BaseEntity <|-- Notification
  Project "*" --> "1" User : owner
  Project "*" --> "0..1" User : reviewer
  Project "1" *-- "1..*" ProposalVersion : versions
  Project "1" o-- "*" Review : reviews
  Project "0..1" o-- "*" AuditLog : projectId
  Review "*" --> "1" ProposalVersion : proposalVersion
  Review "*" --> "1" User : reviewer
  AuditLog "*" --> "1" User : actorId
  ProposalVersion "*" --> "1" User : createdBy
  Notification "*" --> "1" User : recipient
  Notification "*" --> "0..1" Project : project
  User "*" --> "1" Role : role
  Project "*" --> "1" ProjectStatus : status
  ProposalVersion "*" --> "1" ProposalVersionState : state
  Review "*" --> "1" ReviewDecision : decision
  AuditLog "*" --> "1" AuditAction : action
  AuditLog "*" --> "0..2" ProjectStatus : fromStatus, toStatus
  Notification "*" --> "1" NotificationType : type
  ProposalDiff "1" *-- "6" FieldDiff : fields
  User ..> StudentEmail : student()
  ProposalVersion ..> ProposalContent : content()
  ProposalDiffService ..> ProposalVersion : compares two
  ProposalDiffService ..> ProposalDiff : creates
  AuditLogBuilder ..> AuditLog : build()
  note for ProposalVersion "Composition: a version is created with and for one project,<br/>and project_id is NOT NULL. Immutable once SUBMITTED."
  note for AuditLog "projectId and actorId are plain UUIDs, not JPA relations.<br/>A database trigger makes the table append-only."
```

## 3. Workflow engine: State pattern

```mermaid
classDiagram
  class ProjectState {
    <<interface>>
    +status() ProjectStatus
    +availableActions() Set~Action~
    +submit(Project project, Actor actor, ProposalVersion draft)
    +assignReviewer(Project project, User faculty, Actor actor)
    +reassignReviewer(Project project, User faculty, Actor actor)
    +approve(Project project, Actor actor)
    +requestRevision(Project project, Actor actor, String comments)
    +reject(Project project, Actor actor, String comments)
  }
  class DraftState {
    +status() ProjectStatus
    +availableActions() Set~Action~
    +submit(Project project, Actor actor, ProposalVersion draft)
  }
  class SubmittedState {
    +status() ProjectStatus
    +availableActions() Set~Action~
    +assignReviewer(Project project, User faculty, Actor actor)
  }
  class UnderReviewState {
    +status() ProjectStatus
    +availableActions() Set~Action~
    +reassignReviewer(Project project, User faculty, Actor actor)
    +approve(Project project, Actor actor)
    +requestRevision(Project project, Actor actor, String comments)
    +reject(Project project, Actor actor, String comments)
  }
  class RevisionRequestedState {
    +status() ProjectStatus
    +availableActions() Set~Action~
    +submit(Project project, Actor actor, ProposalVersion draft)
  }
  class ApprovedState {
    +status() ProjectStatus
    +availableActions() Set~Action~
  }
  class RejectedState {
    +status() ProjectStatus
    +availableActions() Set~Action~
  }
  class ProjectStateFactory {
    <<Factory>>
    -Map~ProjectStatus,ProjectState~ states
    +stateOf(ProjectStatus status) ProjectState
  }
  class WorkflowService {
    <<Facade>>
    -ProjectStore projectStore
    -ProjectStateFactory stateFactory
    -ProposalService proposalService
    -UserService userService
    -ApplicationEventPublisher events
    +submit(UUID projectId, Actor actor) Project
    +assignReviewer(UUID projectId, UUID facultyId, Actor actor) Project
    +reassignReviewer(UUID projectId, UUID facultyId, Actor actor) Project
    +approve(UUID projectId, Actor actor) Project
    +requestRevision(UUID projectId, Actor actor, String comments) Project
    +reject(UUID projectId, Actor actor, String comments) Project
    -transition(Project project, Actor actor, Action action, Consumer~ProjectState~ step, Map~String,String~ details) Project
  }
  class Guards {
    <<utility>>
    +requireOwner(Project project, Actor actor)$
    +requireRole(Actor actor, Role role)$
    +requireAssignedReviewer(Project project, Actor actor)$
    +requireComments(String comments, Action action)$
    +requireOpenDraft(ProposalVersion draft)$
    +requireComplete(ProposalVersion draft)$
  }
  class AllowedActionsResolver {
    -ProjectStateFactory stateFactory
    +resolve(Project project, Actor actor, boolean hasOpenDraft) Set~Action~
  }
  class Action {
    <<enumeration>>
    SUBMIT
    ASSIGN_REVIEWER
    APPROVE
    REQUEST_REVISION
    REJECT
    REASSIGN_REVIEWER
    EDIT_DRAFT
    CREATE_REVISION
  }
  class IllegalTransitionException {
    -ProjectStatus from
    -Action action
    +getFrom() ProjectStatus
    +getAction() Action
  }
  class ProjectStatusChangedEvent {
    <<record>>
    UUID projectId
    UUID actorId
    Action action
    ProjectStatus fromStatus
    ProjectStatus toStatus
    Map~String,String~ details
  }
  class ReviewerReassignedEvent {
    <<record>>
    UUID projectId
    UUID actorId
    UUID previousReviewerId
    UUID newReviewerId
  }
  class ProjectStore {
    +getById(UUID projectId) Project
    +save(Project project) Project
  }
  class AuditEventListener {
    <<Observer>>
    +on(ProjectStatusChangedEvent event)
    +on(ReviewerReassignedEvent event)
  }
  class NotificationListener {
    <<Observer>>
    +on(ProjectStatusChangedEvent event)
    +on(ReviewerReassignedEvent event)
  }
  ProjectState <|.. DraftState
  ProjectState <|.. SubmittedState
  ProjectState <|.. UnderReviewState
  ProjectState <|.. RevisionRequestedState
  ProjectState <|.. ApprovedState
  ProjectState <|.. RejectedState
  ProjectStateFactory "1" o--> "6" ProjectState : states
  WorkflowService "1" --> "1" ProjectStateFactory : stateFactory
  WorkflowService "1" --> "1" ProjectStore : projectStore
  AllowedActionsResolver "1" --> "1" ProjectStateFactory : stateFactory
  WorkflowService ..> ProjectStatusChangedEvent : publishes
  WorkflowService ..> ReviewerReassignedEvent : publishes
  AuditEventListener ..> ProjectStatusChangedEvent : listens, same transaction
  AuditEventListener ..> ReviewerReassignedEvent : listens, same transaction
  NotificationListener ..> ProjectStatusChangedEvent : listens, after commit
  NotificationListener ..> ReviewerReassignedEvent : listens, after commit
  DraftState ..> Guards
  SubmittedState ..> Guards
  UnderReviewState ..> Guards
  RevisionRequestedState ..> Guards
  ProjectState ..> IllegalTransitionException : throws by default
  ProjectState ..> Action
  note for ProjectState "Every action is a default method that throws<br/>IllegalTransitionException. A state overrides<br/>only its legal actions."
  note for ApprovedState "APPROVED and REJECTED are terminal:<br/>they override no action."
```

## 4. Project state diagram with guards

```mermaid
stateDiagram-v2
  [*] --> DRAFT : createProject [actor is STUDENT, v1 draft created]
  DRAFT --> SUBMITTED : submit [actor is owner, open draft, required fields filled]
  SUBMITTED --> UNDER_REVIEW : assignReviewer [actor is COORDINATOR, assignee has FACULTY role]
  UNDER_REVIEW --> APPROVED : approve [actor is assigned FACULTY reviewer]
  UNDER_REVIEW --> REVISION_REQUESTED : requestRevision [assigned FACULTY reviewer, comments mandatory]
  UNDER_REVIEW --> REJECTED : reject [assigned FACULTY reviewer, comments mandatory]
  UNDER_REVIEW --> UNDER_REVIEW : reassignReviewer [actor is COORDINATOR, new reviewer has FACULTY role and is not the current one]
  REVISION_REQUESTED --> UNDER_REVIEW : submit (resubmit) [actor is owner, open draft vN+1, required fields filled, reviewer set]
  APPROVED --> [*]
  REJECTED --> [*]
  DRAFT : allowed SUBMIT, EDIT_DRAFT
  SUBMITTED : allowed ASSIGN_REVIEWER
  UNDER_REVIEW : allowed APPROVE, REQUEST_REVISION, REJECT, REASSIGN_REVIEWER
  REVISION_REQUESTED : allowed CREATE_REVISION, EDIT_DRAFT, SUBMIT
  APPROVED : terminal
  REJECTED : terminal
  note right of UNDER_REVIEW
    Any action not on an arrow throws IllegalTransitionException (409 ILLEGAL_TRANSITION).
    Guard failures: 403 FORBIDDEN (who), 400 VALIDATION_FAILED (comments, fields, assignee role), 409 ILLEGAL_TRANSITION (no open draft, no reviewer).
  end note
```

## 5. Sequence: create project and submit v1

```mermaid
sequenceDiagram
  autonumber
  actor Student
  participant PC as ProjectController
  participant PS as ProjectService
  participant PRS as ProposalService
  participant PVF as ProposalVersionFactory
  participant WS as WorkflowService
  participant ST as ProjectStore
  participant PSF as ProjectStateFactory
  participant DS as DraftState
  participant AEL as AuditEventListener
  participant DB as PostgreSQL
  Student->>PC: POST /api/projects {title, description, proposal}
  PC->>PS: createProject(newProject, proposal, actor)
  PS->>DB: save Project with status DRAFT
  PS->>AEL: ProjectCreatedEvent
  AEL->>DB: insert audit PROJECT_CREATED
  PS->>PRS: createInitialDraft(project, proposal, actor)
  PRS->>PVF: initialDraft(project, content, createdBy)
  PVF-->>PRS: ProposalVersion v1 (DRAFT)
  PRS->>DB: save v1
  PRS->>AEL: ProposalVersionCreatedEvent
  AEL->>DB: insert audit VERSION_CREATED
  PS-->>PC: ProjectView with allowedActions SUBMIT, EDIT_DRAFT
  PC-->>Student: 201 ProjectDetailResponse
  Student->>PC: POST /api/projects/{id}/submit
  PC->>WS: submit(projectId, actor)
  WS->>ST: getById(projectId)
  WS->>PRS: findOpenDraft(projectId)
  PRS-->>WS: v1
  WS->>PSF: stateOf(DRAFT)
  PSF-->>WS: DraftState
  WS->>DS: submit(project, actor, v1)
  DS->>DS: Guards.requireOwner, requireOpenDraft, requireComplete
  DS->>DS: v1.submit() makes the version immutable, project.moveTo(SUBMITTED)
  WS->>ST: save(project)
  WS->>AEL: ProjectStatusChangedEvent from DRAFT to SUBMITTED
  AEL->>DB: insert audit PROPOSAL_SUBMITTED
  WS->>DB: commit, the UPDATE checks Project.version
  PC->>PS: getProject(id, actor)
  PC-->>Student: 200 ProjectDetailResponse with status SUBMITTED
```

## 6. Sequence: assign, review, request revision

```mermaid
sequenceDiagram
  autonumber
  actor Coordinator
  actor Faculty
  participant PC as ProjectController
  participant RC as ReviewController
  participant WS as WorkflowService
  participant US as UserService
  participant ST as ProjectStore
  participant SS as SubmittedState
  participant RS as ReviewService
  participant PRS as ProposalService
  participant RH as RequestRevisionDecisionHandler
  participant URS as UnderReviewState
  participant AEL as AuditEventListener
  participant DB as PostgreSQL
  Coordinator->>PC: POST /api/projects/{id}/assign {facultyId}
  PC->>WS: assignReviewer(projectId, facultyId, actor)
  WS->>ST: getById(projectId)
  WS->>US: getById(facultyId)
  WS->>SS: assignReviewer(project, faculty, actor)
  SS->>SS: Guards.requireRole(COORDINATOR), the assignee has the FACULTY role
  SS->>SS: project.assignReviewer(faculty), project.moveTo(UNDER_REVIEW)
  WS->>ST: save(project)
  WS->>AEL: ProjectStatusChangedEvent from SUBMITTED to UNDER_REVIEW
  AEL->>DB: insert audit REVIEWER_ASSIGNED
  PC-->>Coordinator: 200 ProjectDetailResponse with status UNDER_REVIEW
  Faculty->>RC: POST /api/projects/{id}/reviews {decision REQUEST_REVISION, comments, versionNumber 1}
  RC->>RS: recordReview(projectId, REQUEST_REVISION, comments, 1, actor)
  RS->>PRS: findLatestSubmitted(projectId)
  PRS-->>RS: v1, and versionNumber must match it
  RS->>RH: apply(projectId, actor, comments)
  RH->>WS: requestRevision(projectId, actor, comments)
  WS->>URS: requestRevision(project, actor, comments)
  URS->>URS: Guards.requireAssignedReviewer, requireComments
  URS->>URS: project.moveTo(REVISION_REQUESTED)
  WS->>ST: save(project)
  WS->>AEL: ProjectStatusChangedEvent from UNDER_REVIEW to REVISION_REQUESTED
  AEL->>DB: insert audit STATUS_CHANGED
  RS->>DB: save Review pointing at v1
  RS->>AEL: ReviewRecordedEvent
  AEL->>DB: insert audit REVIEW_RECORDED
  RC-->>Faculty: 201 ReviewResponse
```

## 7. Sequence: revise, resubmit, approve

```mermaid
sequenceDiagram
  autonumber
  actor Student
  actor Faculty
  participant PRC as ProposalController
  participant PRS as ProposalService
  participant PVF as ProposalVersionFactory
  participant PC as ProjectController
  participant RC as ReviewController
  participant WS as WorkflowService
  participant RRS as RevisionRequestedState
  participant RS as ReviewService
  participant AH as ApproveDecisionHandler
  participant URS as UnderReviewState
  participant AEL as AuditEventListener
  participant DB as PostgreSQL
  Student->>PRC: POST /api/projects/{id}/proposals
  PRC->>PRS: createNextVersion(projectId, actor)
  PRS->>PRS: requireOwner, the state allows CREATE_REVISION, no open draft
  PRS->>PVF: nextDraftFrom(v1, createdBy)
  PVF-->>PRS: v2 (DRAFT), a copy of the v1 content
  PRS->>DB: save v2, the v1 row stays untouched
  PRS->>AEL: ProposalVersionCreatedEvent
  AEL->>DB: insert audit VERSION_CREATED
  PRC-->>Student: 201 ProposalVersionResponse v2
  Student->>PRC: PUT /api/projects/{id}/proposal {edited fields}
  PRC->>PRS: updateDraft(projectId, content, actor)
  PRS->>DB: save v2
  PRS->>AEL: ProposalDraftUpdatedEvent
  AEL->>DB: insert audit DRAFT_UPDATED
  Student->>PC: POST /api/projects/{id}/submit
  PC->>WS: submit(projectId, actor)
  WS->>RRS: submit(project, actor, v2)
  RRS->>RRS: Guards.requireOwner, requireOpenDraft, requireComplete, reviewer is set
  RRS->>RRS: v2.submit(), project.moveTo(UNDER_REVIEW)
  WS->>DB: save project
  WS->>AEL: ProjectStatusChangedEvent from REVISION_REQUESTED to UNDER_REVIEW
  AEL->>DB: insert audit PROPOSAL_SUBMITTED
  PC-->>Student: 200 with status UNDER_REVIEW and the same reviewer
  Faculty->>RC: POST /api/projects/{id}/reviews {decision APPROVE, versionNumber 2}
  RC->>RS: recordReview(projectId, APPROVE, null, 2, actor)
  RS->>AH: apply(projectId, actor, null)
  AH->>WS: approve(projectId, actor)
  WS->>URS: approve(project, actor)
  URS->>URS: Guards.requireAssignedReviewer, project.moveTo(APPROVED)
  WS->>DB: save project
  WS->>AEL: ProjectStatusChangedEvent from UNDER_REVIEW to APPROVED
  AEL->>DB: insert audit STATUS_CHANGED
  RS->>DB: save Review pointing at v2
  RS->>AEL: ReviewRecordedEvent
  AEL->>DB: insert audit REVIEW_RECORDED
  RC-->>Faculty: 201 ReviewResponse
```

## 8. Sequence: illegal transition rejected

```mermaid
sequenceDiagram
  autonumber
  actor Faculty
  participant RC as ReviewController
  participant RS as ReviewService
  participant PRS as ProposalService
  participant AH as ApproveDecisionHandler
  participant WS as WorkflowService
  participant PSF as ProjectStateFactory
  participant SS as SubmittedState
  participant GEH as GlobalExceptionHandler
  participant DB as PostgreSQL
  Faculty->>RC: POST /api/projects/{id}/reviews {decision APPROVE}
  RC->>RS: recordReview(projectId, APPROVE, null, null, actor)
  RS->>PRS: findLatestSubmitted(projectId)
  PRS-->>RS: v1
  RS->>AH: apply(projectId, actor, null)
  AH->>WS: approve(projectId, actor)
  WS->>PSF: stateOf(SUBMITTED)
  PSF-->>WS: SubmittedState
  WS->>SS: approve(project, actor)
  SS-->>WS: throws IllegalTransitionException(SUBMITTED, APPROVE), the inherited default method
  Note over WS,DB: No save, no event, no review row: the transaction rolls back
  WS-->>RC: the exception propagates
  RC->>GEH: handleBusiness(ex, request)
  GEH-->>Faculty: 409 {code ILLEGAL_TRANSITION, message Action APPROVE is not allowed while the project is SUBMITTED}
```

## 9. ER diagram

```mermaid
erDiagram
  users ||--o{ projects : "owner_id"
  users |o--o{ projects : "reviewer_id"
  projects ||--|{ proposal_versions : "project_id"
  users ||--o{ proposal_versions : "created_by"
  projects ||--o{ reviews : "project_id"
  proposal_versions ||--o{ reviews : "proposal_version_id"
  users ||--o{ reviews : "reviewer_id"
  projects |o--o{ audit_logs : "project_id"
  users ||--o{ audit_logs : "actor_id"
  users ||--o{ notifications : "recipient_id"
  projects |o--o{ notifications : "project_id"
  users {
    UUID id PK "users_pkey"
    VARCHAR(120) name
    VARCHAR(255) email UK "users_email_key"
    VARCHAR(100) password_hash
    VARCHAR(20) role "users_role_check: STUDENT, FACULTY, COORDINATOR"
    TIMESTAMPTZ created_at
    INTEGER admission_year "nullable, idx_users_admission_year_branch with branch"
    VARCHAR(20) branch "nullable"
  }
  projects {
    UUID id PK "projects_pkey"
    VARCHAR(200) title
    TEXT description
    VARCHAR(500) tech_stack "nullable"
    TEXT team_members "nullable"
    UUID owner_id FK "projects_owner_id_fkey, idx_projects_owner"
    UUID reviewer_id FK "nullable, projects_reviewer_id_fkey, idx_projects_reviewer"
    VARCHAR(30) status "projects_status_check: DRAFT, SUBMITTED, UNDER_REVIEW, REVISION_REQUESTED, APPROVED, REJECTED; idx_projects_status"
    BIGINT version "DEFAULT 0, optimistic lock"
    TIMESTAMPTZ created_at
    TIMESTAMPTZ updated_at "idx_projects_updated_at"
  }
  proposal_versions {
    UUID id PK "proposal_versions_pkey"
    UUID project_id FK "proposal_versions_project_id_fkey; uq_proposal_project_version with version_number; ux_proposal_one_open_draft WHERE state = 'DRAFT'"
    INTEGER version_number "proposal_versions_version_number_check: at least 1"
    TEXT problem_statement "nullable"
    TEXT objectives "nullable"
    TEXT methodology "nullable"
    TEXT expected_outcome "nullable"
    TEXT timeline "nullable"
    VARCHAR(500) reference_link "nullable"
    VARCHAR(20) state "proposal_versions_state_check: DRAFT, SUBMITTED; trg_proposal_versions_immutable blocks UPDATE and DELETE when SUBMITTED"
    TIMESTAMPTZ submitted_at "nullable, ck_proposal_submitted_at: set exactly when SUBMITTED"
    UUID created_by FK "proposal_versions_created_by_fkey"
    TIMESTAMPTZ created_at
  }
  reviews {
    UUID id PK "reviews_pkey"
    UUID project_id FK "reviews_project_id_fkey, idx_reviews_project"
    UUID proposal_version_id FK "reviews_proposal_version_id_fkey"
    UUID reviewer_id FK "reviews_reviewer_id_fkey"
    VARCHAR(30) decision "reviews_decision_check: APPROVE, REQUEST_REVISION, REJECT"
    TEXT comments "nullable, ck_review_comments: required unless APPROVE"
    TIMESTAMPTZ created_at
  }
  audit_logs {
    UUID id PK "audit_logs_pkey; trg_audit_logs_append_only blocks UPDATE and DELETE"
    UUID project_id FK "nullable, audit_logs_project_id_fkey; idx_audit_logs_project with created_at"
    UUID actor_id FK "audit_logs_actor_id_fkey; idx_audit_logs_actor with created_at"
    VARCHAR(40) action "idx_audit_logs_action with created_at"
    VARCHAR(30) from_status "nullable"
    VARCHAR(30) to_status "nullable"
    TEXT details "nullable, JSON text"
    TIMESTAMPTZ created_at "idx_audit_logs_created_at"
  }
  notifications {
    UUID id PK "notifications_pkey"
    UUID recipient_id FK "notifications_recipient_id_fkey; idx_notifications_recipient_read with is_read; idx_notifications_recipient_created with created_at"
    UUID project_id FK "nullable, notifications_project_id_fkey"
    VARCHAR(30) type "notifications_type_check: REVIEWER_ASSIGNED, REVIEWER_UNASSIGNED, REVIEW_DECISION, RESUBMITTED"
    VARCHAR(500) message
    BOOLEAN is_read "DEFAULT false"
    TIMESTAMPTZ created_at
  }
```

## 10. Component / architecture

```mermaid
flowchart TB
  subgraph Browser
    FE["prastav-frontend<br/>(React + TS, Vite, TanStack Query, Axios)"]
  end
  subgraph BE["prastav-backend (Spring Boot 3, Java 17)"]
    SEC["Security<br/>(JwtAuthenticationFilter, SecurityConfig, @PreAuthorize)"]
    subgraph WL["web layer"]
      WEB["Controllers + DTO mappers<br/>(Auth, User, Project, Dashboard, Proposal, Review, Audit, Notification)<br/>GlobalExceptionHandler"]
    end
    subgraph MODS["modules"]
      AUTH["auth<br/>(UserService, AuthService, JwtService, EmailPolicy)"]
      PROJ["project<br/>(ProjectService, ProjectStore, ProjectAccessPolicy,<br/>ProjectHistoryService, DashboardService)"]
      PROP["proposal<br/>(ProposalService, ProposalVersionFactory, ProposalDiffService)"]
      REV["review<br/>(ReviewService, ReviewDecisionHandler x3)"]
      WF["workflow<br/>(WorkflowService, ProjectState x6, ProjectStateFactory, AllowedActionsResolver)"]
      AUD["audit<br/>(AuditEventListener, AuditRecorder, AuditQueryService)"]
      NOTIF["notification<br/>(NotificationListener, NotificationDispatcher, NotificationChannel x2, NotificationService)"]
    end
    REPO["Spring Data JPA repositories"]
    FLY["Flyway migrations"]
  end
  DB[("PostgreSQL 16<br/>(docker-compose)")]
  FE -->|"REST + Bearer JWT (/api, proxied by Vite)"| SEC
  SEC --> WEB
  WEB --> AUTH
  WEB --> PROJ
  WEB --> PROP
  WEB --> REV
  WEB --> WF
  WEB --> AUD
  WEB --> NOTIF
  REV -->|"decision strategies"| WF
  REV -->|"ProjectStore, ProjectAccessPolicy"| PROJ
  REV -->|"latest submitted version"| PROP
  REV -->|"UserService"| AUTH
  WF -->|"ProjectStore"| PROJ
  WF -->|"open draft"| PROP
  WF -->|"UserService"| AUTH
  PROP -->|"ProjectStore, ProjectAccessPolicy"| PROJ
  PROP -->|"ProjectStateFactory"| WF
  PROJ -->|"proposal versions"| PROP
  PROJ -->|"AllowedActionsResolver"| WF
  PROJ -->|"UserService"| AUTH
  PROJ -->|"timeline reviews"| REV
  PROJ -->|"timeline status changes"| AUD
  NOTIF -->|"ProjectStore"| PROJ
  NOTIF -->|"UserService"| AUTH
  WF -.->|"events"| AUD
  PROJ -.->|"events"| AUD
  PROP -.->|"events"| AUD
  REV -.->|"events"| AUD
  WF -.->|"events, after commit"| NOTIF
  REV -.->|"events, after commit"| NOTIF
  AUTH --> REPO
  PROJ --> REPO
  PROP --> REPO
  REV --> REPO
  AUD --> REPO
  NOTIF --> REPO
  REPO --> DB
  FLY --> DB
```

## 11. Observer and Strategy (domain events, audit, review decisions)

```mermaid
classDiagram
  class ProjectCreatedEvent {
    <<record>>
    UUID projectId
    UUID actorId
    String title
  }
  class ProposalVersionCreatedEvent {
    <<record>>
    UUID projectId
    int versionNumber
    UUID actorId
  }
  class ProposalDraftUpdatedEvent {
    <<record>>
    UUID projectId
    int versionNumber
    UUID actorId
  }
  class ProjectStatusChangedEvent {
    <<record>>
    UUID projectId
    UUID actorId
    Action action
    ProjectStatus fromStatus
    ProjectStatus toStatus
    Map~String,String~ details
  }
  class ReviewerReassignedEvent {
    <<record>>
    UUID projectId
    UUID actorId
    UUID previousReviewerId
    UUID newReviewerId
  }
  class ReviewRecordedEvent {
    <<record>>
    UUID projectId
    UUID reviewId
    int versionNumber
    UUID reviewerId
    ReviewDecision decision
  }
  class AuditEventListener {
    <<Observer>>
    -AuditRecorder recorder
    -ObjectMapper objectMapper
    +on(ProjectCreatedEvent event)
    +on(ProposalVersionCreatedEvent event)
    +on(ProposalDraftUpdatedEvent event)
    +on(ProjectStatusChangedEvent event)
    +on(ReviewerReassignedEvent event)
    +on(ReviewRecordedEvent event)
  }
  class AuditRecorder {
    <<interface>>
    +record(AuditLog entry)
  }
  class JpaAuditRecorder {
    -AuditLogRepository auditLogRepository
    +record(AuditLog entry)
  }
  class NotificationListener {
    <<Observer>>
    +on(ProjectStatusChangedEvent event)
    +on(ReviewerReassignedEvent event)
    +on(ReviewRecordedEvent event)
  }
  class ReviewDecisionHandler {
    <<interface>>
    +decision() ReviewDecision
    +apply(UUID projectId, Actor actor, String comments)
  }
  class ApproveDecisionHandler {
    -WorkflowService workflowService
  }
  class RequestRevisionDecisionHandler {
    -WorkflowService workflowService
  }
  class RejectDecisionHandler {
    -WorkflowService workflowService
  }
  class ReviewService {
    -Map~ReviewDecision,ReviewDecisionHandler~ handlers
    +recordReview(UUID projectId, ReviewDecision decision, String comments, Integer versionNumber, Actor actor) Review
    +listReviews(UUID projectId, Actor actor) List~Review~
  }
  class WorkflowService {
    <<Facade>>
    +approve(UUID projectId, Actor actor) Project
    +requestRevision(UUID projectId, Actor actor, String comments) Project
    +reject(UUID projectId, Actor actor, String comments) Project
  }
  AuditRecorder <|.. JpaAuditRecorder
  AuditEventListener "1" --> "1" AuditRecorder : recorder
  AuditEventListener ..> ProjectCreatedEvent
  AuditEventListener ..> ProposalVersionCreatedEvent
  AuditEventListener ..> ProposalDraftUpdatedEvent
  AuditEventListener ..> ProjectStatusChangedEvent
  AuditEventListener ..> ReviewerReassignedEvent
  AuditEventListener ..> ReviewRecordedEvent
  NotificationListener ..> ProjectStatusChangedEvent
  NotificationListener ..> ReviewerReassignedEvent
  NotificationListener ..> ReviewRecordedEvent
  ReviewDecisionHandler <|.. ApproveDecisionHandler
  ReviewDecisionHandler <|.. RequestRevisionDecisionHandler
  ReviewDecisionHandler <|.. RejectDecisionHandler
  ReviewService "1" o--> "3" ReviewDecisionHandler : handlers
  ReviewService ..> ReviewRecordedEvent : publishes
  ApproveDecisionHandler "1" --> "1" WorkflowService : approve()
  RequestRevisionDecisionHandler "1" --> "1" WorkflowService : requestRevision()
  RejectDecisionHandler "1" --> "1" WorkflowService : reject()
  note for AuditEventListener "Runs in the publisher's transaction: a change<br/>and its audit row commit or roll back together."
  note for NotificationListener "Runs after commit. Each delivery has its own transaction.<br/>See the notification class and sequence diagrams."
```

## 12. Sequence: notification dispatch

```mermaid
sequenceDiagram
  autonumber
  participant RS as ReviewService
  participant AEL as AuditEventListener
  participant NL as NotificationListener
  participant ST as ProjectStore
  participant ND as NotificationDispatcher
  participant RNC as RetryingNotificationChannel
  participant IA as InAppNotificationChannel
  participant NS as NotificationService
  participant EC as EmailNotificationChannel
  participant DB as PostgreSQL
  RS->>DB: save Review
  RS->>AEL: ReviewRecordedEvent, handled now in the same transaction
  AEL->>DB: insert audit REVIEW_RECORDED
  Note over RS,NL: Spring holds the event for NotificationListener until the transaction ends
  alt the transaction commits
    RS->>NL: on(ReviewRecordedEvent) after commit, the request's connection already returned
    NL->>ST: getById(projectId)
    ST-->>NL: Project
    NL->>NL: new Notification(owner, project, REVIEW_DECISION, message)
    NL->>ND: dispatch(notification)
    loop each enabled channel in configured order
      ND->>RNC: deliver(notification)
      alt channel in-app
        loop up to maxAttempts while delivery throws, pausing delay between attempts
          RNC->>IA: deliver(notification)
          IA->>NS: create(recipient, project, type, message)
          NS->>DB: new REQUIRES_NEW transaction: saveAndFlush runs the INSERT, then commit
          Note over RNC,DB: A failing INSERT throws here, inside the retry loop, and rolls back only this attempt
        end
      else channel email
        loop up to maxAttempts while delivery throws, pausing delay between attempts
          RNC->>EC: deliver(notification)
          EC->>EC: log.info of the message that would be mailed
        end
      end
      RNC-->>ND: returns, or rethrows the last failure, which NotificationDispatcher logs at WARN before the next channel
    end
    Note over NL,DB: Each attempt and each notification has its own transaction, so a failure cannot reach the committed review, its audit entry or another notification.
  else the transaction rolls back
    Note over RS,NL: NotificationListener is not called and nobody is notified
  end
```

## 13. Notification channels (Strategy and Decorator)

```mermaid
classDiagram
  class NotificationListener {
    <<Observer>>
    -ProjectStore projectStore
    -UserService userService
    -NotificationDispatcher dispatcher
    +on(ProjectStatusChangedEvent event)
    +on(ReviewerReassignedEvent event)
    +on(ReviewRecordedEvent event)
  }
  class NotificationDispatcher {
    -List~NotificationChannel~ enabledChannels
    +dispatch(Notification notification)
  }
  class NotificationChannel {
    <<interface>>
    +name() String
    +deliver(Notification notification)
  }
  class InAppNotificationChannel {
    +String NAME$
    -NotificationService notificationService
    +name() String
    +deliver(Notification notification)
  }
  class EmailNotificationChannel {
    +String NAME$
    +name() String
    +deliver(Notification notification)
  }
  class RetryingNotificationChannel {
    <<Decorator>>
    -NotificationChannel delegate
    -int maxAttempts
    -Duration delay
    +name() String
    +deliver(Notification notification)
  }
  class NotificationProperties {
    <<record>>
    List~String~ enabledChannels
    Retry retry
  }
  class NotificationPropertiesRetry["NotificationProperties.Retry"] {
    <<record>>
    int maxAttempts
    Duration delay
  }
  class NotificationService {
    -NotificationRepository notificationRepository
    +create(User recipient, Project project, NotificationType type, String message) Notification
    +unreadCount(Actor actor) long
    +markRead(UUID notificationId, Actor actor) Notification
    +markAllRead(Actor actor) int
  }
  class NotificationRepository {
    <<interface>>
    +countByRecipientIdAndReadFalse(UUID recipientId) long
    +markAllReadFor(UUID recipientId) int
  }
  class Notification {
    -User recipient
    -Project project
    -NotificationType type
    -String message
    -boolean read
    +markRead()
    +isAddressedTo(UUID userId) boolean
  }
  class NotificationType {
    <<enumeration>>
    REVIEWER_ASSIGNED
    REVIEWER_UNASSIGNED
    REVIEW_DECISION
    RESUBMITTED
  }
  NotificationListener "1" --> "1" NotificationDispatcher : dispatcher
  NotificationDispatcher "1" o--> "*" NotificationChannel : enabledChannels
  NotificationChannel <|.. InAppNotificationChannel
  NotificationChannel <|.. EmailNotificationChannel
  NotificationChannel <|.. RetryingNotificationChannel
  RetryingNotificationChannel "1" o--> "1" NotificationChannel : delegate
  NotificationDispatcher ..> RetryingNotificationChannel : wraps each enabled channel
  NotificationDispatcher ..> NotificationProperties : enabled-channels, retry
  NotificationProperties "1" *-- "1" NotificationPropertiesRetry : retry
  InAppNotificationChannel "1" --> "1" NotificationService : notificationService
  NotificationService "1" --> "1" NotificationRepository : notificationRepository
  NotificationListener ..> Notification : creates
  NotificationRepository ..> Notification : stores
  Notification "*" --> "1" NotificationType : type
  note for NotificationListener "After commit, no transaction of its own.<br/>ASSIGN_REVIEWER and SUBMIT from REVISION_REQUESTED: reviewer.<br/>Review decision: owner.<br/>Reassignment: previous and new reviewer."
  note for RetryingNotificationChannel "Retries a delivery that throws. The last failure is rethrown,<br/>and the dispatcher logs it and moves to the next channel."
```

## 14. Auth view (EmailPolicy Strategy)

```mermaid
classDiagram
  class User {
    -String name
    -String email
    -String passwordHash
    -Role role
    -Integer admissionYear
    -String branch
    +student(String name, StudentEmail studentEmail, String passwordHash)$ User
    +toActor() Actor
  }
  class Role {
    <<enumeration>>
    STUDENT
    FACULTY
    COORDINATOR
  }
  class Actor {
    <<record>>
    UUID userId
    Role role
    +hasRole(Role expected) boolean
  }
  class EmailPolicy {
    <<interface>>
    +requireInstitutional(String email) String
    +requireStudent(String email) StudentEmail
  }
  class PatternEmailPolicy {
    -InstitutionProperties institution
    -String domainSuffix
    -Pattern studentLocalPart
    +requireInstitutional(String email) String
    +requireStudent(String email) StudentEmail
  }
  class InstitutionProperties {
    <<record>>
    String name
    String emailDomain
    String studentEmailPattern
  }
  class StudentEmail {
    <<record>>
    String email
    int admissionYear
    String branch
  }
  class AuthService {
    -EmailPolicy emailPolicy
    +register(String name, String email, String password, Role role) AuthResult
    +login(String email, String password) AuthResult
  }
  class UserService {
    -EmailPolicy emailPolicy
    +getById(UUID id) User
    +listByRole(Role role) List~User~
    +createAccount(Actor actor, String name, String email, String password, Role role) User
  }
  class DemoDataSeeder {
    -EmailPolicy emailPolicy
    +run(ApplicationArguments args)
  }
  class JwtService {
    +generateToken(User user) String
    +parse(String token) Optional~Actor~
  }
  class AuthResult {
    <<record>>
    String token
    User user
  }
  EmailPolicy <|.. PatternEmailPolicy
  PatternEmailPolicy "1" --> "1" InstitutionProperties : institution
  AuthService "1" --> "1" EmailPolicy : requireStudent()
  UserService "1" --> "1" EmailPolicy : requireInstitutional()
  DemoDataSeeder "1" --> "1" EmailPolicy : emailPolicy
  AuthService "1" --> "1" JwtService : jwtService
  EmailPolicy ..> StudentEmail : parses
  User ..> StudentEmail : student()
  User ..> Actor : toActor()
  JwtService ..> Actor : parse()
  AuthService ..> AuthResult : returns
  User "*" --> "1" Role : role
  Actor "*" --> "1" Role : role
  note for EmailPolicy "Another naming scheme is a new implementation;<br/>the services depend only on this interface."
```

## 15. Activity: submit, assign, review, revise or approve

```mermaid
flowchart TD
  start([Start])
  subgraph Student
    create["Create project with draft v1"]
    edit["Edit the draft"]
    submit["Submit the proposal"]
    filled{"Required fields filled?"}
    revreq["Project is REVISION_REQUESTED, read the feedback"]
    next["Create version vN+1, prefilled from vN"]
    resubmit["Edit the draft and resubmit"]
  end
  subgraph Coordinator
    assign["Project is SUBMITTED, assign a FACULTY reviewer"]
  end
  subgraph Faculty
    review["Project is UNDER_REVIEW, review the latest submitted version"]
    decision{"Decision"}
    approved["Project is APPROVED"]
    rejected["Project is REJECTED"]
  end
  stop1([End])
  stop2([End])
  start --> create --> edit --> submit --> filled
  filled -->|"no, 400 VALIDATION_FAILED"| edit
  filled -->|yes| assign --> review --> decision
  decision -->|APPROVE| approved --> stop1
  decision -->|"REJECT, comments mandatory"| rejected --> stop2
  decision -->|"REQUEST_REVISION, comments mandatory"| revreq --> next --> resubmit
  resubmit -->|"Required fields filled, returns to the same reviewer"| review
  review -.- reassign["The coordinator can reassign the reviewer here, the status stays UNDER_REVIEW"]
```
