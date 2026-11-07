#!/usr/bin/env bash
# Proves the checks can fail: plants one fault at a time in a temporary copy of the repository and expects a report.
set -u
here="$(cd "$(dirname "$0")" && pwd)"
repo="$(cd "$here/../.." && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
export DOCS_CHECK_ROOT="$work"
mkdir -p "$work/backend" "$work/frontend"
cp -R "$repo/README.md" "$repo/docs" "$repo/scripts" "$repo/docker-compose.yml" "$work/"
cp -R "$repo/backend/src" "$repo/backend/Dockerfile" "$work/backend/"
cp -R "$repo/frontend/src" "$repo/frontend/.env.example" "$repo/frontend/Dockerfile" "$repo/frontend/nginx.conf" "$work/frontend/"

failures=0
run() { (cd "$here" && python3 "$1" >"$work/out" 2>&1); }
run check_names.py && run check_diagrams.py && run check_refs.py || { echo "FAIL: the unchanged copy should pass"; cat "$work/out"; exit 1; }

# plant NAME CHECK FILE SED_EXPRESSION EXPECTED_OUTPUT
plant() {
  local name="$1" check="$2" file="$work/$3" expression="$4" expected="$5"
  cp "$file" "$work/original"
  sed -i.bak -E "$expression" "$file"; rm -f "$file.bak"
  if cmp -s "$file" "$work/original"; then
    echo "FAIL  $name: the fault was not planted"; failures=$((failures + 1))
  elif run "$check"; then
    echo "FAIL  $name: the check passed"; failures=$((failures + 1))
  elif ! grep -q "$expected" "$work/out"; then
    echo "FAIL  $name: no report of $expected"; failures=$((failures + 1))
  else
    echo "ok    $name"
  fi
  cp "$work/original" "$file"
}

plant "unknown class in the docs" check_names.py README.md '1,/WorkflowService/s/WorkflowService/WorkflowServiceX/' WorkflowServiceX
plant "unknown method" check_names.py docs/LLD.md '1,/`ProjectService.listProjects`/s/`ProjectService.listProjects`/`ProjectService.listAllProjects`/' listAllProjects
plant "unknown endpoint" check_names.py docs/LLD.md '1,/`GET \/api\/audit`/s/`GET \/api\/audit`/`GET \/api\/audits`/' 'GET /audits is not an endpoint'
plant "unknown schema name" check_names.py docs/PRD.md '1,/project\\_id/s/project\\_id/project\\_key/' project_key
plant "missing file" check_names.py README.md '1,/`docs\/LLD.md`/s/`docs\/LLD.md`/`docs\/designs\/x.md`/' designs
plant "code gained an endpoint" check_names.py backend/src/main/java/com/prastav/audit/web/AuditController.java 's/@GetMapping$/@GetMapping("\/all")/' /api/audit/all
plant "catalogue member removed from code" check_names.py backend/src/main/java/com/prastav/audit/AuditQueryService.java 's/findByProject\(/findForProject(/' findByProject
plant "column missing from section 8" check_names.py backend/src/main/resources/db/migration/V3__user_institution_profile.sql 's/ADD COLUMN branch /ADD COLUMN branch_code /' branch_code
plant "class in one notation only" check_diagrams.py docs/uml/13_notification_classes.puml 's/^class EmailNotificationChannel \{/class EmailChannelRenamed {/' 13_notification_classes
plant "association without multiplicity" check_diagrams.py docs/uml/02_domain_class.puml 's/^Project "\*" --> "1" User : owner/Project --> User : owner/' 02_domain_class
plant "sequence message differs" check_diagrams.py docs/uml/08_seq_illegal_transition.puml 's/stateOf\(SUBMITTED\)/stateFor(SUBMITTED)/' 08_seq
plant "index missing from the ER diagram" check_diagrams.py backend/src/main/resources/db/migration/V6__audit_query_indexes.sql 's/idx_audit_logs_actor/idx_audit_logs_who/' idx_audit_logs_who
plant "foreign key not drawn" check_diagrams.py docs/uml/10_er.puml '/^users \|\|--o\{ proposal_versions/d' 10_er
plant "dangling section reference" check_refs.py docs/INTERVIEW_PREP.md 's/LLD section 2\.4\.1/LLD section 9.9/' "section 9.9"
plant "broken image" check_refs.py docs/LLD.md '1,/10_er\.png/s/10_er\.png/10_erd.png/' 10_erd
plant "banned word" check_refs.py README.md '1,3s/workflow/planned workflow/' "banned word"

[ "$failures" -eq 0 ] && echo "PASS: every planted fault was reported" || { echo "FAILED: $failures"; exit 1; }
