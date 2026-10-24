# Prastav

Academic project workflow and approval system: students submit structured project proposals, a coordinator assigns a faculty reviewer, and the reviewer approves, requests a revision or rejects. Every submitted version is immutable and every action is audited.

This repository is the **Milestone 1** submission: a working vertical slice of the full lifecycle (FR-1 to FR-11) plus the low level design and UML for the whole system, with Milestone 2 features designed but not implemented.

| Part | Location |
|---|---|
| Backend (`prastav-backend`): Spring Boot 3, Java 17, Maven | `backend/` |
| Frontend (`prastav-frontend`): React 18 + TypeScript, Vite | `frontend/` |
| Database: PostgreSQL 16 via Docker | `docker-compose.yml` |
| Low level design (whole system, M1/M2 tagged) | `docs/LLD.md`, `docs/LLD.pdf` |
| UML: PlantUML sources and PNGs | `docs/uml/*.puml`, `docs/uml/png/` |
| UML: Mermaid | `docs/diagrams.md`, renders in `docs/uml/mermaid-png/` |
| Viva preparation and change guide | `docs/INTERVIEW_PREP.md` |
| Two-person commit plan | `COMMIT_PLAN.md` |

## Prerequisites

- JDK 17 or newer, Maven 3.9+
- Node.js 20+ and npm
- Docker (for PostgreSQL), or a local PostgreSQL 16 with database/user/password `prastav`

## Run it

```bash
docker compose up -d                              # PostgreSQL on localhost:5432
cd backend && mvn spring-boot:run                 # API on http://localhost:8080 (Flyway migrates, demo users are seeded)
cd frontend && npm install && npm run dev         # UI on http://localhost:5173 (in a second terminal)
```

- UI: http://localhost:5173
- Swagger UI: http://localhost:8080/swagger-ui.html (log in via `POST /api/auth/login`, then click **Authorize** and paste the token)

### Demo accounts

All seeded accounts use the password **Password@123**.

| Role | Email |
|---|---|
| Coordinator | coordinator@prastav.edu |
| Faculty | anita.rao@prastav.edu, vikram.shah@prastav.edu |
| Student | aarav@prastav.edu, diya@prastav.edu, rohan@prastav.edu |

The login page has a "Demo accounts" list that fills these in.

### Configuration

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/prastav` | JDBC URL |
| `DB_USER` / `DB_PASSWORD` | `prastav` / `prastav` | database credentials |
| `JWT_SECRET` | development-only value | HS256 key, at least 32 bytes; set your own outside a demo |
| `CORS_ORIGIN` | `http://localhost:5173` | allowed browser origin |
| `SEED_DEMO_DATA` | `true` | create demo users on an empty database |

## Tests

```bash
cd backend && mvn test        # JUnit 5 + Mockito unit tests and MockMvc slice tests; no database needed
cd frontend && npm run build  # type-check (tsc) and production build
```

Unit tests cover every state class (legal and illegal transitions), `ProjectStateFactory`, `AllowedActionsResolver`, `WorkflowService`, `ReviewService`, proposal versioning, `ProjectService` and the audit listener. `ProjectControllerWebTest` checks 401, 403 and 409 error mapping through the real security configuration.

## Five-minute demo script

1. Sign in as **aarav@prastav.edu**, create a project, fill all proposal fields, then **Submit v1 for review**.
2. Sign in as **coordinator@prastav.edu**, open the project, choose **Dr. Anita Rao**, **Assign reviewer**.
3. Sign in as **anita.rao@prastav.edu**, choose **Request revision**, write a comment, **Record decision**.
4. As the student: read the feedback, **Start version 2**, edit, save, **Resubmit v2**. Use the v1/v2 tabs to show v1 is unchanged.
5. As the faculty member: **Approve**.
6. In Swagger: `GET /api/projects/{id}/history` shows every step; then `POST /api/projects/{id}/reviews` with `{"decision":"APPROVE"}` on a project that is only SUBMITTED returns **409 ILLEGAL_TRANSITION**.
7. Walk through `ProjectState`, `UnderReviewState`, `WorkflowService` and `SubmittedStateTest` (see `docs/INTERVIEW_PREP.md`, part C).

## Project structure

```
backend/src/main/java/com/prastav/
  common/     error model, exceptions, BaseEntity, Swagger config
  auth/       users, JWT, security, seed data   (+ web/)
  workflow/   State pattern engine: ProjectState, six states, factory, WorkflowService
  project/    Project aggregate, access policy, history   (+ web/)
  proposal/   immutable versions, factory, draft editing  (+ web/)
  review/     review decisions as strategies               (+ web/)
  audit/      append-only audit log fed by domain events
backend/src/main/resources/db/migration/V1__init.sql
frontend/src/  api/ auth/ components/ pages/
docs/          LLD.md, LLD.pdf, diagrams.md, INTERVIEW_PREP.md, uml/
```

## Milestone 2 (designed, not implemented)

Notifications through a `NotificationChannel` strategy, version compare, reviewer reassignment, coordinator dashboard, audit-log viewer, list filters/search/pagination, Dockerfiles for backend and frontend, GitHub Actions CI and Testcontainers integration tests. Their design is in `docs/LLD.md` (tagged **[M2 Planned]**) and in the diagrams (grey, dashed, `<<M2>>`).

## Troubleshooting

- **Backend fails at startup with a connection error**: PostgreSQL is not running; check `docker compose ps`.
- **Port 5432 already in use**: stop the local PostgreSQL or change the left side of the port mapping and `DB_URL`.
- **"Flyway validate failed" after editing V1**: never edit an applied migration; add `V2__...sql`, or for a throwaway demo database run `docker compose down -v`.
- **UI says the server is not reachable**: the backend is not on port 8080; Vite proxies `/api` there.
- **401 after an hour**: the JWT expired; sign in again.
