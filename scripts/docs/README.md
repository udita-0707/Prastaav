# Documentation checks

These scripts compare the documentation with the code. They need `python3` and nothing else, and they do not touch the database or the running application.

```bash
scripts/docs/run_all.sh     # all three checks; exit code 1 if any finds a problem
scripts/docs/selftest.sh    # plants faults in a temporary copy and confirms each one is reported
```

| Script | What it checks |
|---|---|
| `check_names.py` | Every class, method, endpoint, table, column, index, constraint, migration, constant and path named in `README.md`, `docs/LLD.md`, `docs/PRD.md`, `docs/INTERVIEW_PREP.md`, `docs/VIVA.md` and `docs/diagrams.md` exists in the code. The class catalogue, API table and schema section of `docs/LLD.md` are compared in both directions. |
| `check_diagrams.py` | Every diagram exists in `docs/uml/*.puml` and `docs/diagrams.md` with the same classes, members, relations, messages, states and labels, and has both PNGs. Class members and sequence messages exist in the code, associations have multiplicities, and the ER diagram matches the migrations. |
| `check_refs.py` | Every "section N" points at a heading of `docs/LLD.md`, every link and image exists, and the banned-words rule from `CLAUDE.md` holds. |

`code_index.py` holds what the checks share: it reads the Java sources, the controllers and the Flyway migrations.

`allowed_names.txt` lists the names the docs may use that the code does not contain, such as product names. Keep it short: a stale name added there stops being checked.

## What the checks cannot see

They check names and structure, not meaning. A sentence that names real classes but describes them wrongly passes. Library types count as existing if the code uses them. In `docs/INTERVIEW_PREP.md` Part B, which describes changes a reader could make, only the files not marked new are checked.
