# Prastav diagrams (Mermaid)

The same diagrams as the PlantUML sources in `docs/uml/*.puml` (rendered PNGs in `docs/uml/png/`).
Names match the code exactly, and every element exists in the code.
Mermaid renders of these blocks are in `docs/uml/mermaid-png/`.

## 1. Use cases

```mermaid
flowchart LR
  Student([Student])
  Faculty([Faculty])
  Coordinator([Coordinator])
  UC1["Register / log in (JWT)"]
  UC2["Create project with draft v1"]
  UC3["Edit own draft"]
  UC4["Submit / resubmit proposal"]
  UC5["View feedback and history"]
  UC6["Create revision vN+1"]
  UC7["Assign or reassign faculty reviewer"]
  UC8["Review latest submitted version"]
  UC9["View project timeline"]
  UC10["View all projects"]
  UC11["Filter, search and page project lists"]
  UC12["View dashboard counts by status"]
  UC13["Create faculty or coordinator account"]
  UC14["Read and mark own notifications"]
  UC15["View coordinator dashboard (awaiting assignment, reviewer workload)"]
  Student --> UC1 & UC2 & UC3 & UC4 & UC5 & UC6 & UC9 & UC11 & UC12 & UC14
  Faculty --> UC1 & UC8 & UC5 & UC11 & UC12 & UC14
  Coordinator --> UC1 & UC7 & UC10 & UC9 & UC11 & UC12 & UC13 & UC14 & UC15
```

## 2. Domain class diagram

```mermaid
classDiagram
  class BaseEntity {
    <<abstract>>
    -UUID id
    -Instant createdAt
    -boolean newEntity
    +getId() UUID
    +isNew() boolean
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
  }
  class StudentEmail {
    <<record>>
    String email
    int admissionYear
    String branch
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
  }
  class InstitutionProperties {
    <<record>>
    String name
    String emailDomain
    String studentEmailPattern
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
    problemStatement
    objectives
    methodology
    expectedOutcome
    timeline
    referenceLink
    +empty()$ ProposalContent
  }
  class ProposalDiff {
    <<record>>
    fromVersion
    toVersion
    fields
  }
  class FieldDiff {
    <<record>>
    name
    fromValue
    toValue
    changed
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
    +builder()$ Builder
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
  }
  class NotificationType {
    <<enumeration>>
    REVIEWER_ASSIGNED
    REVIEWER_UNASSIGNED
    REVIEW_DECISION
    RESUBMITTED
  }
  BaseEntity <|-- User
  BaseEntity <|-- Project
  BaseEntity <|-- ProposalVersion
  BaseEntity <|-- Review
  BaseEntity <|-- AuditLog
  BaseEntity <|-- Notification
  User --> Role
  User ..> StudentEmail : student()
  EmailPolicy <|.. PatternEmailPolicy
  PatternEmailPolicy --> InstitutionProperties
  EmailPolicy ..> StudentEmail : parses
  Project "*" --> "1" User : owner
  Project "*" --> "0..1" User : reviewer
  Project --> ProjectStatus
  Project "1" *-- "1..*" ProposalVersion : versions
  ProposalVersion --> ProposalVersionState
  ProposalVersion ..> ProposalContent
  ProposalVersion "1" -- "*" Review
  ProposalDiffService ..> ProposalVersion : compares two
  ProposalDiffService ..> ProposalDiff : creates
  ProposalDiff "1" *-- "6" FieldDiff : fields
  Review --> ReviewDecision
  Review "*" --> "1" User : reviewer
  AuditLog --> AuditAction
  Notification --> NotificationType
  Notification "*" --> "1" User : recipient
  Notification "*" --> "0..1" Project : project
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
  class DraftState { +submit() }
  class SubmittedState { +assignReviewer() }
  class UnderReviewState { +approve() +requestRevision() +reject() +reassignReviewer() }
  class RevisionRequestedState { +submit() }
  class ApprovedState
  class RejectedState
  ProjectState <|.. DraftState
  ProjectState <|.. SubmittedState
  ProjectState <|.. UnderReviewState
  ProjectState <|.. RevisionRequestedState
  ProjectState <|.. ApprovedState
  ProjectState <|.. RejectedState
  class ProjectStateFactory {
    <<Factory>>
    -Map~ProjectStatus,ProjectState~ states
    +stateOf(ProjectStatus status) ProjectState
  }
  class WorkflowService {
    <<Facade>>
    +submit(UUID projectId, Actor actor) Project
    +assignReviewer(UUID projectId, UUID facultyId, Actor actor) Project
    +reassignReviewer(UUID projectId, UUID facultyId, Actor actor) Project
    +approve(UUID projectId, Actor actor) Project
    +requestRevision(UUID projectId, Actor actor, String comments) Project
    +reject(UUID projectId, Actor actor, String comments) Project
    -transition(project, actor, action, step, details) Project
  }
  class Guards {
    <<utility>>
    +requireOwner(Project, Actor)$
    +requireRole(Actor, Role)$
    +requireAssignedReviewer(Project, Actor)$
    +requireComments(String, Action)$
    +requireOpenDraft(ProposalVersion)$
    +requireComplete(ProposalVersion)$
  }
  class AllowedActionsResolver {
    +resolve(Project project, Actor actor, boolean hasOpenDraft) Set~Action~
  }
  class IllegalTransitionException {
    -ProjectStatus from
    -Action action
  }
  class ProjectStatusChangedEvent {
    <<record>>
    projectId
    actorId
    action
    fromStatus
    toStatus
    details
  }
  class ReviewerReassignedEvent {
    <<record>>
    projectId
    actorId
    previousReviewerId
    newReviewerId
  }
  ProjectStateFactory o--> ProjectState
  WorkflowService --> ProjectStateFactory
  WorkflowService --> ProjectStore
  WorkflowService ..> ProjectStatusChangedEvent : publishes
  WorkflowService ..> ReviewerReassignedEvent : publishes
  AllowedActionsResolver --> ProjectStateFactory
  ProjectState ..> IllegalTransitionException : throws
  DraftState ..> Guards
  UnderReviewState ..> Guards
```

## 4. Project state diagram with guards

```mermaid
stateDiagram-v2
  [*] --> DRAFT : createProject (v1 draft)
  DRAFT --> SUBMITTED : submit [owner, open draft, fields filled]
  SUBMITTED --> UNDER_REVIEW : assignReviewer [COORDINATOR, assignee FACULTY]
  UNDER_REVIEW --> APPROVED : approve [assigned faculty]
  UNDER_REVIEW --> REVISION_REQUESTED : requestRevision [assigned faculty, comments]
  UNDER_REVIEW --> REJECTED : reject [assigned faculty, comments]
  UNDER_REVIEW --> UNDER_REVIEW : reassignReviewer [COORDINATOR, new reviewer FACULTY and not the current one]
  REVISION_REQUESTED --> UNDER_REVIEW : submit (resubmit) [owner, vN+1 complete, reviewer set]
  APPROVED --> [*]
  REJECTED --> [*]
  note right of UNDER_REVIEW
    Anything not drawn throws IllegalTransitionException (409).
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
  participant DS as DraftState
  participant AEL as AuditEventListener
  participant DB as PostgreSQL
  Student->>PC: POST /api/projects
  PC->>PS: createProject(newProject, proposal, actor)
  PS->>DB: save(Project DRAFT)
  PS->>AEL: ProjectCreatedEvent
  AEL->>DB: audit PROJECT_CREATED
  PS->>PRS: createInitialDraft(project, content, actor)
  PRS->>PVF: initialDraft(project, content, userId)
  PRS->>DB: save(v1 DRAFT)
  PRS->>AEL: ProposalVersionCreatedEvent
  PC-->>Student: 201 ProjectDetailResponse
  Student->>PC: POST /api/projects/{id}/submit
  PC->>WS: submit(id, actor)
  WS->>PRS: findOpenDraft(id)
  WS->>DS: submit(project, actor, v1)
  DS->>DS: Guards + v1.submit() + moveTo(SUBMITTED)
  WS->>DB: save(project) with @Version
  WS->>AEL: ProjectStatusChangedEvent(DRAFT to SUBMITTED)
  AEL->>DB: audit PROPOSAL_SUBMITTED
  PC-->>Student: 200 (SUBMITTED)
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
  participant SS as SubmittedState
  participant RS as ReviewService
  participant RH as RequestRevisionDecisionHandler
  participant URS as UnderReviewState
  participant AEL as AuditEventListener
  Coordinator->>PC: POST /api/projects/{id}/assign {facultyId}
  PC->>WS: assignReviewer(id, facultyId, actor)
  WS->>SS: assignReviewer(project, faculty, actor)
  SS->>SS: requireRole(COORDINATOR), faculty is FACULTY, moveTo(UNDER_REVIEW)
  WS->>AEL: ProjectStatusChangedEvent (REVIEWER_ASSIGNED)
  Faculty->>RC: POST /api/projects/{id}/reviews {REQUEST_REVISION, comments}
  RC->>RS: recordReview(id, decision, comments, versionNumber, actor)
  RS->>RS: latest submitted version check
  RS->>RH: apply(id, actor, comments)
  RH->>WS: requestRevision(id, actor, comments)
  WS->>URS: requestRevision(project, actor, comments)
  URS->>URS: requireAssignedReviewer, requireComments, moveTo(REVISION_REQUESTED)
  WS->>AEL: ProjectStatusChangedEvent (STATUS_CHANGED)
  RS->>RS: save Review(v1)
  RS->>AEL: ReviewRecordedEvent (REVIEW_RECORDED)
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
  participant WS as WorkflowService
  participant RRS as RevisionRequestedState
  participant RS as ReviewService
  participant AH as ApproveDecisionHandler
  participant URS as UnderReviewState
  Student->>PRC: POST /api/projects/{id}/proposals
  PRC->>PRS: createNextVersion(id, actor)
  PRS->>PVF: nextDraftFrom(v1, userId)
  PVF-->>PRS: v2 DRAFT (copy of v1, v1 untouched)
  Student->>PRC: PUT /api/projects/{id}/proposal
  PRC->>PRS: updateDraft(id, content, actor)
  Student->>WS: POST /submit -> submit(id, actor)
  WS->>RRS: submit(project, actor, v2)
  RRS->>RRS: Guards, reviewer set, v2.submit(), moveTo(UNDER_REVIEW)
  Faculty->>RS: recordReview(id, APPROVE, null, 2, actor)
  RS->>AH: apply(id, actor, null)
  AH->>WS: approve(id, actor)
  WS->>URS: approve(project, actor)
  URS->>URS: requireAssignedReviewer, moveTo(APPROVED)
  RS->>RS: save Review(v2), ReviewRecordedEvent
```

## 8. Sequence: illegal transition rejected

```mermaid
sequenceDiagram
  autonumber
  actor Faculty
  participant RC as ReviewController
  participant RS as ReviewService
  participant AH as ApproveDecisionHandler
  participant WS as WorkflowService
  participant PSF as ProjectStateFactory
  participant SS as SubmittedState
  participant GEH as GlobalExceptionHandler
  Faculty->>RC: POST /api/projects/{id}/reviews {APPROVE}
  RC->>RS: recordReview(...)
  RS->>AH: apply(...)
  AH->>WS: approve(id, actor)
  WS->>PSF: stateOf(SUBMITTED)
  WS->>SS: approve(project, actor)
  SS-->>WS: IllegalTransitionException(SUBMITTED, APPROVE)
  Note over WS: no save, no event, no review, transaction rolls back
  WS-->>GEH: exception propagates
  GEH-->>Faculty: 409 {code: ILLEGAL_TRANSITION}
```

## 9. ER diagram

```mermaid
erDiagram
  users ||--o{ projects : owns
  users |o--o{ projects : reviews
  projects ||--|{ proposal_versions : has
  proposal_versions ||--o{ reviews : "reviewed in"
  projects ||--o{ reviews : has
  users ||--o{ reviews : writes
  projects |o--o{ audit_logs : "logged in"
  users ||--o{ audit_logs : "acts in"
  users ||--o{ notifications : receives
  projects |o--o{ notifications : about
  users {
    UUID id PK
    VARCHAR name
    VARCHAR email UK
    VARCHAR password_hash
    VARCHAR role
    INTEGER admission_year "nullable; idx_users_admission_year_branch with branch"
    VARCHAR branch "nullable"
    TIMESTAMPTZ created_at
  }
  projects {
    UUID id PK
    VARCHAR title
    TEXT description
    VARCHAR tech_stack
    TEXT team_members
    UUID owner_id FK "idx_projects_owner"
    UUID reviewer_id FK "idx_projects_reviewer"
    VARCHAR status "idx_projects_status"
    BIGINT version
    TIMESTAMPTZ created_at
    TIMESTAMPTZ updated_at "idx_projects_updated_at"
  }
  proposal_versions {
    UUID id PK
    UUID project_id FK "ux_proposal_one_open_draft (WHERE state = DRAFT)"
    INTEGER version_number "uq_proposal_project_version with project_id"
    TEXT problem_statement
    TEXT objectives
    TEXT methodology
    TEXT expected_outcome
    TEXT timeline
    VARCHAR reference_link
    VARCHAR state
    TIMESTAMPTZ submitted_at
    UUID created_by FK
    TIMESTAMPTZ created_at
  }
  reviews {
    UUID id PK
    UUID project_id FK "idx_reviews_project"
    UUID proposal_version_id FK
    UUID reviewer_id FK
    VARCHAR decision
    TEXT comments
    TIMESTAMPTZ created_at
  }
  audit_logs {
    UUID id PK
    UUID project_id FK "idx_audit_logs_project with created_at"
    UUID actor_id FK
    VARCHAR action
    VARCHAR from_status
    VARCHAR to_status
    TEXT details
    TIMESTAMPTZ created_at
  }
  notifications {
    UUID id PK
    UUID recipient_id FK "idx_notifications_recipient_read with is_read; idx_notifications_recipient_created with created_at"
    UUID project_id FK "nullable"
    VARCHAR type
    VARCHAR message
    BOOLEAN is_read
    TIMESTAMPTZ created_at
  }
```

## 10. Component / architecture

```mermaid
flowchart TB
  FE["prastav-frontend<br/>React + TS, Vite, TanStack Query, Axios"]
  subgraph BE["prastav-backend: Spring Boot 3, Java 17"]
    SEC["Security: JwtAuthenticationFilter, SecurityConfig, @PreAuthorize"]
    WEB["Controllers + DTO mappers, GlobalExceptionHandler"]
    AUTH["auth: UserService, AuthService, JwtService"]
    PROJ["project: ProjectService, ProjectStore, ProjectAccessPolicy, ProjectHistoryService, DashboardService"]
    PROP["proposal: ProposalService, ProposalVersionFactory"]
    REV["review: ReviewService, ReviewDecisionHandler x3"]
    WF["workflow: WorkflowService, ProjectState x6, ProjectStateFactory"]
    AUD["audit: AuditEventListener, AuditRecorder, AuditQueryService"]
    NOTIF["notification: NotificationListener, NotificationDispatcher, NotificationService"]
    REPO["Spring Data JPA repositories"]
    FLY["Flyway migrations"]
  end
  DB[("PostgreSQL 16 (docker-compose)")]
  FE -->|"REST + Bearer JWT"| SEC --> WEB
  WEB --> AUTH & PROJ & PROP & REV & WF & NOTIF
  REV -->|strategies| WF
  PROJ --> PROP
  PROP -->|allowed actions| WF
  WF -.->|events| AUD
  PROJ -.->|events| AUD
  PROP -.->|events| AUD
  REV -.->|events| AUD
  WF -.->|events, after commit| NOTIF
  REV -.->|events, after commit| NOTIF
  NOTIF --> PROJ
  AUTH & PROJ & PROP & REV & AUD --> REPO
  NOTIF --> REPO
  REPO --> DB
  FLY --> DB
```

## 11. Observer, Strategy and Decorator (events, review decisions, email policy, notification channels and retries)

```mermaid
classDiagram
  class AuditEventListener {
    -AuditRecorder recorder
    -ObjectMapper objectMapper
    +on(ProjectCreatedEvent)
    +on(ProposalVersionCreatedEvent)
    +on(ProposalDraftUpdatedEvent)
    +on(ProjectStatusChangedEvent)
    +on(ReviewerReassignedEvent)
    +on(ReviewRecordedEvent)
  }
  class AuditRecorder {
    <<interface>>
    +record(AuditLog entry)
  }
  class JpaAuditRecorder
  AuditRecorder <|.. JpaAuditRecorder
  AuditEventListener --> AuditRecorder
  class NotificationListener {
    -ProjectStore projectStore
    -UserService userService
    -NotificationDispatcher dispatcher
    +on(ProjectStatusChangedEvent)
    +on(ReviewerReassignedEvent)
    +on(ReviewRecordedEvent)
  }
  note for NotificationListener "@TransactionalEventListener (after commit), REQUIRES_NEW.<br/>ASSIGN_REVIEWER and SUBMIT from REVISION_REQUESTED notify the reviewer;<br/>a review decision notifies the owner;<br/>a reassignment notifies the previous and the new reviewer"
  NotificationListener --> NotificationDispatcher : dispatch()
  class ReviewDecisionHandler {
    <<interface>>
    +decision() ReviewDecision
    +apply(UUID projectId, Actor actor, String comments)
  }
  class ApproveDecisionHandler
  class RequestRevisionDecisionHandler
  class RejectDecisionHandler
  ReviewDecisionHandler <|.. ApproveDecisionHandler
  ReviewDecisionHandler <|.. RequestRevisionDecisionHandler
  ReviewDecisionHandler <|.. RejectDecisionHandler
  class ReviewService {
    -Map~ReviewDecision,ReviewDecisionHandler~ handlers
    +recordReview(projectId, decision, comments, versionNumber, actor) Review
    +listReviews(UUID projectId, Actor actor) List~Review~
  }
  ReviewService o--> ReviewDecisionHandler
  ApproveDecisionHandler --> WorkflowService
  RequestRevisionDecisionHandler --> WorkflowService
  RejectDecisionHandler --> WorkflowService
  class EmailPolicy {
    <<interface>>
    +requireInstitutional(String email) String
    +requireStudent(String email) StudentEmail
  }
  class PatternEmailPolicy
  EmailPolicy <|.. PatternEmailPolicy
  class AuthService {
    +register(name, email, password, role) AuthResult
  }
  class UserService {
    +createAccount(actor, name, email, password, role) User
  }
  AuthService --> EmailPolicy : requireStudent()
  UserService --> EmailPolicy : requireInstitutional()
  class NotificationChannel {
    <<interface>>
    +name() String
    +deliver(Notification notification)
  }
  class InAppNotificationChannel {
    -NotificationService notificationService
  }
  class EmailNotificationChannel
  NotificationChannel <|.. InAppNotificationChannel
  NotificationChannel <|.. EmailNotificationChannel
  class RetryingNotificationChannel {
    <<Decorator>>
    -NotificationChannel delegate
    -int maxAttempts
    -Duration delay
  }
  NotificationChannel <|.. RetryingNotificationChannel
  RetryingNotificationChannel o--> NotificationChannel : delegate
  class NotificationDispatcher {
    -List~NotificationChannel~ enabledChannels
    +dispatch(Notification notification)
  }
  class NotificationProperties {
    <<record>>
    +enabledChannels() List~String~
    +retry() Retry
  }
  NotificationDispatcher o--> RetryingNotificationChannel : wraps each enabled channel
  NotificationDispatcher ..> NotificationProperties : enabled-channels, retry
  class NotificationService {
    +create(recipient, project, type, message) Notification
  }
  InAppNotificationChannel --> NotificationService : create()
```
