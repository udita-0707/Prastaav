# Prastaav

**Ideas → Proposals → Impact.** Prastaav is an academic project proposal workflow and approval system. Students write structured project proposals, the coordinator assigns a faculty reviewer, and the reviewer approves, requests changes or rejects. Every submitted version is immutable, every review points to the exact version it judged, and every important action is recorded in an append-only audit log.

## Features by role

**Student**
- Register with your institutional student email (`<firstname>.<YY><branch>@nst.rishihood.edu.in`); your admission year and branch are read from it.
- Create a project and write the proposal in a four-step wizard (basic info; problem and objectives; methodology and outcomes; timeline and review), saving drafts at any step.
- Submit the proposal; the submitted version is frozen.
- Follow the status on the home dashboard (totals, drafts, in review, approved) and get an alert when a reviewer requests changes.
- Read the reviewer's feedback, start the next version (prefilled from the last one) and resubmit it to the same reviewer.
- Browse earlier versions, reviews and the full timeline of a project.

**Faculty**
- See the projects assigned to you, with counts of pending reviews and projects awaiting student changes.
- Review the latest submitted version: approve, request changes or reject. Comments are required for the last two.
- A "Reviews" list shows what waits for your decision, the longest-waiting first.

**Coordinator**
- See every project with counts by status, search, status and faculty filters, sorting and paging.
- Assign a faculty reviewer to submitted projects from the "Assign Faculty" page, from any list row or from the project page.
- Follow every review in progress from the "Reviews" list.
- Create faculty and coordinator accounts on the institutional domain through `POST /api/users`.

### Screens

| Screen | Route | Who |
| --- | --- | --- |
| Landing page | `/` | everyone (signed-in users go to `/app`) |
| Sign in, with demo-account buttons | `/login` | everyone |
| Create an account | `/register` | students |
| Home dashboard | `/app` | each role has its own |
| Project list (My / Assigned / All Projects) | `/app/projects` | all roles, scoped by role |
| Create project wizard | `/app/projects/new` | student |
| Project page: overview, versions, reviews, timeline, next step | `/app/projects/:id` | participants |
| Edit draft or revision | `/app/projects/:id/edit` | student owner |
| Reviews (Under Review projects) | `/app/reviews` | faculty, coordinator |
| Assign faculty | `/app/assign` | coordinator |

The mockups the screens follow are in [`docs/design`](docs/design/README.md).

## Architecture

A **modular monolith**: one Spring Boot application and one PostgreSQL database, organised by module (`auth`, `project`, `proposal`, `review`, `workflow`, `audit`, `notification`, `common`). The workflow engine uses the State pattern: each project status is a class that owns its legal actions, so an illegal transition is rejected with `409 ILLEGAL_TRANSITION`. Domain events feed the audit log. The API returns `allowedActions` for the caller with every project, and the web app only shows the buttons the backend allows.

The web app is a React single-page app. Pages are composed from a small design system (`frontend/src/components/ui`), data comes from the REST API through TanStack Query, and list filters live in the URL so a filtered list can be shared. See `docs/LLD.md` for the full design, including the frontend architecture.

## Tech stack

| Part | Technology |
| --- | --- |
| Backend (`prastav-backend`) | Java 17, Spring Boot 3.3, Spring Security (JWT), Spring Data JPA, Flyway, springdoc-openapi |
| Database | PostgreSQL 16 (Docker Compose for development) |
| Frontend (`prastav-frontend`) | React 18, TypeScript, Vite, React Router, TanStack Query, Axios |
| UI | Tailwind CSS 3, lucide-react icons, Plus Jakarta Sans (self-hosted through Fontsource) |
| Tests | JUnit 5, Mockito, MockMvc |

## Institution configuration

The institution profile lives under `prastav.institution` in `backend/src/main/resources/application.yml` and is bound to `InstitutionProperties`. `PatternEmailPolicy` applies it to every new account.

| Property (variable) | Default | Purpose |
| --- | --- | --- |
| `name` (`INSTITUTION_NAME`) | `Newton School of Technology, Rishihood University` | institution name used in validation messages |
| `email-domain` (`INSTITUTION_EMAIL_DOMAIN`) | `nst.rishihood.edu.in` | the only email domain accepted for any account |
| `student-email-pattern` | `[a-z]+\.(?<year>\d{2})(?<branch>[a-z]+)` | local part of a student email; the `year` group gives admission year 2000 + YY, the `branch` group gives the branch in upper case |

Rules:
- `POST /api/auth/register` creates **students only**. The email must be on the institutional domain and match the student pattern, e.g. `aarav.23csai@nst.rishihood.edu.in` is admission year 2023, branch CSAI. Emails are matched case-insensitively and stored in lower case. Any other email, or a request for another role, is rejected with `400 VALIDATION_FAILED`.
- Faculty and coordinator accounts are created by the coordinator through `POST /api/users` (domain check only) or by the seed.

## Other configuration

### Backend

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_PORT` | `5432` | host port of the Docker Compose database |
| `DB_URL` | `jdbc:postgresql://localhost:5432/prastav` | JDBC URL |
| `DB_USER` / `DB_PASSWORD` | `prastav` / `prastav` | database credentials |
| `JWT_SECRET` | development-only value | HS256 key, at least 32 bytes; set your own outside a demo |
| `CORS_ORIGIN` | `http://localhost:5173` | allowed browser origin |
| `SEED_DEMO_DATA` | `true` | create the demo users on an empty database |
| `DEMO_PASSWORD` | `Password@123` | shared password of every demo account |

### Frontend

Copy `frontend/.env.example` to `frontend/.env` to change these.

| Variable | Default | Purpose |
| --- | --- | --- |
| `VITE_INSTITUTION_NAME` | `Your University` | institution name in the landing page footer |
| `VITE_INSTITUTION_EMAIL_DOMAIN` | `nst.rishihood.edu.in` | domain in the register form hint and the demo accounts; keep it equal to the backend `email-domain` |
| `VITE_SHOW_DEMO_ACCOUNTS` | `true` | show the demo-account buttons on the sign-in page; set `false` when the backend runs without seed data |
| `VITE_API_BASE_URL` | `/api` | API base URL; the default goes through the Vite dev proxy to port 8080 |

The product name and tagline are defined once in `frontend/src/config/brand.ts`, and every image is referenced from `frontend/src/assets/index.ts`, so the logo and illustrations can be replaced without code changes (see [`docs/design/ASSETS.md`](docs/design/ASSETS.md)).

## Demo accounts

All seeded accounts are fictional people and share one password, `prastav.seed.demo-password` (default **Password@123**). The sign-in page has "Try demo accounts" buttons that fill these in.

| Role | Name | Email | Admission year, branch |
| --- | --- | --- | --- |
| Coordinator | Meera Nair | coordinator@nst.rishihood.edu.in | |
| Faculty | Dr. Anita Rao | anita.rao@nst.rishihood.edu.in | |
| Faculty | Dr. Vikram Shah | vikram.shah@nst.rishihood.edu.in | |
| Student | Aarav Mehta | aarav.23csai@nst.rishihood.edu.in | 2023, CSAI |
| Student | Diya Kapoor | diya.23csai@nst.rishihood.edu.in | 2023, CSAI |
| Student | Rohan Iyer | rohan.24csai@nst.rishihood.edu.in | 2024, CSAI |

### Five-minute demo

1. Sign in as **aarav.23csai@nst.rishihood.edu.in**, create a project in the wizard, fill every step, then **Submit proposal**.
2. Sign in as **coordinator@nst.rishihood.edu.in**, open **Assign Faculty**, choose **Dr. Anita Rao** for the project.
3. Sign in as **anita.rao@nst.rishihood.edu.in**, open the project from **Awaiting your review**, choose **Request changes**, write a comment and **Record decision**.
4. As the student: read the feedback, **Start version 2**, edit it with the feedback panel open, then **Resubmit v2**. The Versions tab shows v1 unchanged.
5. As the faculty member: **Approve**.
6. In Swagger: `GET /api/projects/{id}/history` shows every step; `POST /api/projects/{id}/reviews` with `{"decision":"APPROVE"}` on a project that is only SUBMITTED returns **409 ILLEGAL_TRANSITION**.

## Run it

Prerequisites: JDK 17 or newer, Maven 3.9+, Node.js 20+ and Docker. Run each command from the repository root, the second and third in their own terminals:

```bash
docker compose up -d                                              # PostgreSQL 16 on localhost:5432
mvn -f backend/pom.xml spring-boot:run                            # API on http://localhost:8080; Flyway migrates and the demo users are seeded
npm --prefix frontend install && npm --prefix frontend run dev    # web app on http://localhost:5173
```

Open http://localhost:5173 and sign in with a demo account. If port 5432 is taken by a local PostgreSQL, start the database with `DB_PORT=5433 docker compose up -d` and the backend with `DB_URL=jdbc:postgresql://localhost:5433/prastav mvn -f backend/pom.xml spring-boot:run`.

### Troubleshooting

- **Backend fails at startup with a connection error**: PostgreSQL is not running; check `docker compose ps`.
- **Port 5432 already in use**: stop the local PostgreSQL, or set `DB_PORT` and `DB_URL` as shown above.
- **"Flyway validate failed" after editing a migration**: never edit an applied migration; add a new `V<n>__...sql`, or for a throwaway demo database run `docker compose down -v`.
- **The web app says the server is not reachable**: the backend is not on port 8080; Vite proxies `/api` there.
- **Demo sign-in fails with "Invalid email or password"**: the seed only runs on an empty database, so a database that already had other users has no demo accounts; for a throwaway demo database run `docker compose down -v` and start the backend again.
- **Signed out after an hour**: the JWT expired; sign in again.

## API

Swagger UI is at http://localhost:8080/swagger-ui.html (OpenAPI JSON at `/v3/api-docs`): sign in with `POST /api/auth/login`, click **Authorize** and paste the token.

Base path `/api`; every endpoint except register, login and Swagger needs `Authorization: Bearer <jwt>`. Errors share one body: `{ timestamp, status, code, message, path, fieldErrors[] }`. The full contracts are in `docs/LLD.md` section 10.

| Method | Path | Role | Purpose |
| --- | --- | --- | --- |
| POST | `/auth/register`, `/auth/login` | public | Register a student account with an institutional student email; sign in |
| GET | `/users?role=FACULTY` | coordinator | Faculty list for assignment |
| POST | `/users` | coordinator | Create a FACULTY or COORDINATOR account on the institutional domain |
| POST | `/projects` | student | Create a project with draft v1 |
| GET | `/projects` | any | Role-scoped, paged list; optional `status` (one or more), `q` (case-insensitive search), `reviewerId`, `sort` (`LATEST`, `OLDEST`, `TITLE`), `page` (from 0), `size` (1 to 50, default 10); returns `{ content, page, size, totalElements, totalPages }`, and invalid values give `400 VALIDATION_FAILED` |
| GET | `/projects/summary` | any | Counts by status for the caller's projects, plus projects awaiting assignment for the coordinator |
| GET | `/projects/{id}` | participant | Project detail with `allowedActions` |
| PUT | `/projects/{id}/proposal` | student owner | Edit the open draft |
| POST / GET | `/projects/{id}/proposals` | student owner / participant | Start the next version / list versions |
| POST | `/projects/{id}/submit` | student owner | Submit or resubmit |
| POST | `/projects/{id}/assign` | coordinator | Assign a faculty reviewer |
| POST / GET | `/projects/{id}/reviews` | assigned faculty / participant | Record a decision / list reviews |
| GET | `/projects/{id}/history` | participant | Timeline of versions, reviews and status changes |

## Project structure

```
backend/src/main/java/com/prastav/
  common/     error model, exceptions, BaseEntity, Swagger config
  auth/       users, institution email policy, JWT, security, seed data (+ web/)
  workflow/   State pattern engine: ProjectState, six states, factory, WorkflowService
  project/    Project aggregate, access policy, list query and specifications, counts, history (+ web/)
  proposal/   immutable versions, factory, draft editing  (+ web/)
  review/     review decisions as strategies               (+ web/)
  audit/      append-only audit log fed by domain events
  notification/ in-app notifications: entity, per-user inbox queries, read flags (+ web/ DTO and mapper)
backend/src/main/resources/db/migration/   Flyway migrations
frontend/
  public/brand/          logo, logo mark, favicon
  src/api/               Axios client, endpoints, DTO types
  src/auth/              session context and route guard
  src/assets/            illustrations and the single asset index
  src/components/ui/     design system (Button, Input, Card, StatCard, StatusPill, DataTable, Modal, Toast, …)
  src/components/        domain components (WorkflowRail, ReviewPanel, AssignReviewerPanel, ProposalFields, ErrorNote)
  src/config/           brand, navigation, demo accounts
  src/layouts/           AppShell (sidebar, top bar), AuthLayout
  src/lib/               formatting, people helpers, list-parameter and debounce hooks
  src/pages/             landing, auth, role homes, project list, wizard, project page, assign
  src/router.tsx         routes and guards
  tailwind.config.js     design tokens
```

## Documentation

| Document | Contents |
| --- | --- |
| [`docs/PRD.md`](docs/PRD.md) | Requirements and technical specification |
| [`docs/LLD.md`](docs/LLD.md) | Low level design: architecture, class catalogue, schema, API contracts, frontend architecture, tests |
| [`docs/diagrams.md`](docs/diagrams.md), [`docs/uml/`](docs/uml) | Mermaid and PlantUML diagrams |
| [`docs/design/README.md`](docs/design/README.md) | Mockups, design tokens and UI components |
| [`docs/design/ASSETS.md`](docs/design/ASSETS.md) | Every image, its size and where it is used |
| [`docs/INTERVIEW_PREP.md`](docs/INTERVIEW_PREP.md) | Design walkthrough and change guide |

## Tests

```bash
cd backend && mvn test                     # JUnit 5 + Mockito unit tests and MockMvc slice tests; no database needed
cd frontend && npm run typecheck           # TypeScript type check
cd frontend && npm run build               # type check and production build
```

Unit tests cover every workflow state (legal and illegal transitions), `ProjectStateFactory`, `AllowedActionsResolver`, `WorkflowService`, `ReviewService`, proposal versioning, `ProjectService` (including list paging and dashboard counts), `ProjectListQuery` (paging defaults and bounds), `ProjectSpecifications` (role scope and filters), the audit listener, `PatternEmailPolicy` (with the shipped configuration), registration in `AuthService`, account creation in `UserService` and `NotificationService` (inbox scoping, unread count, read ownership). `UserControllerWebTest` checks the coordinator-only `POST /api/users`. `ProjectControllerWebTest` checks 400, 401, 403 and 409 error mapping through the real security configuration, plus the list filters, the page envelope, invalid list parameters and the summary endpoint.

## Design patterns

| Pattern | Where |
| --- | --- |
| State | `ProjectState` and its six status classes |
| Factory | `ProjectStateFactory`, `ProposalVersionFactory` |
| Facade | `WorkflowService` |
| Observer | domain events consumed by `AuditEventListener` |
| Strategy | `ReviewDecisionHandler` and its three decision handlers; `EmailPolicy` with `PatternEmailPolicy` for account emails |
| Specification | `ProjectSpecifications` for the filtered project list |
| Repository | Spring Data repositories per module |
| Builder | `AuditLog.Builder` |
| DTO and mapper | `*Request` / `*Response` records with `*Mapper` classes |
