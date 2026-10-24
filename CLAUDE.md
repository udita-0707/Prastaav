# Prastaav: working rules

Prastav is an academic project proposal workflow and approval system (Spring Boot, React + TypeScript, PostgreSQL). Repo: udita-0707/Prastaav. Backend base package: com.prastav. Package, class, file and folder names already in the repo are the source of truth and must never be renamed.

## Who is working
Every session starts with PERSON = A or B. A is GitHub udita-0707, B is GitHub AmanKumar1411. Before the first commit run `gh api user --jq .login` and `git config user.email`. The login must match the PERSON and the email must be that account's users.noreply.github.com address. If not, stop and tell me to run `as A` or `as B`. Never switch accounts or change git identity yourself. Do only the tasks assigned to my PERSON.

## Git workflow
- Start: `git checkout main && git pull`. Check the prerequisites listed in the prompt are on main (git log, file checks). If any is missing, stop and report; do not work around it.
- Run `cd backend && mvn -q test` and `cd frontend && npm run build` on main first. If main is red, stop and report.
- One branch per task: feat/<task-id>-<short-name> (fix/, docs/, chore/, test/ as appropriate), created from main.
- Small, buildable commits with conventional messages (feat, fix, test, docs, chore, with a scope). Stage explicit paths only. Run `git status` before each commit and never add stray or untracked local files.
- Real timestamps only. Never set GIT_AUTHOR_DATE, GIT_COMMITTER_DATE or --date. No force push. Never push to main. Never merge PRs.
- Do not add Co-Authored-By lines or "Generated with" text to commits or PR bodies.
- Before pushing each task, run `cd backend && mvn -q test` and, for frontend changes, `cd frontend && npm run build`. Fix failures. If something fails twice, stop and report instead of guessing.
- Push the branch and open a PR with `gh pr create`. The PR body lists: what changed, files touched, how to test, which docs were updated.
- End of every session print: tasks done, tasks skipped and why, PR links, and a short manual test checklist.

## Code standards
- Every entity extends BaseEntity. Constructor injection. Small classes. No business logic in controllers. No TODOs, no dead code, no unused abstractions, no features outside the task.
- Every new endpoint has role checks, ownership checks, DTO validation and the existing error codes (GlobalExceptionHandler).
- Every pattern class has Javadoc naming the pattern and why it is used.
- Every new table has a Flyway migration. Add indexes for the columns that are filtered or joined.
- Every task includes tests for the logic it adds (JUnit 5 + Mockito, MockMvc for controllers).
- Demo and seed data use fictional people only. Never commit real emails, tokens or secrets. Configuration comes from application.yml with safe development defaults.

## Documentation rules (docs are updated in the same PR as the code)
- Prastav is described as one product. Never use the words M1, M2, milestone, phase 1, phase 2, planned, upcoming, TODO or FlowForge in README.md, docs/, code comments or Javadoc.
- Docs describe only what exists in code. Names in diagrams, LLD and README must match the code exactly.
- Each PR updates every doc it affects: README.md (features, API, run guide, demo accounts), docs/PRD.md (requirements), docs/LLD.md and docs/lld/ (design), docs/uml/*.puml and docs/diagrams.md (keep PlantUML and Mermaid in sync), and the ER diagram if the schema changed.
- Before opening a PR run: grep -rniE "\bM1\b|\bM2\b|milestone|phase [12]|planned|upcoming|TODO|FlowForge" README.md docs backend/src frontend/src and fix every hit that is not a legitimate word in context.
- README is a single narrative: overview, features by role, architecture, tech stack, institution configuration, demo accounts, 3-command run guide, API docs (Swagger URL), project structure, docs index, tests, design patterns summary.
