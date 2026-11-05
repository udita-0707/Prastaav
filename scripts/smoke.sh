#!/usr/bin/env bash
# End-to-end smoke test of the Prastav API. Needs bash, curl and jq, and a running backend with the demo accounts seeded.
#
#   scripts/smoke.sh                                  # backend on http://localhost:8080
#   BASE_URL=http://localhost:8099 scripts/smoke.sh
#
# Every run creates its own uniquely named projects and one new student, so it can run repeatedly. It marks the
# notifications of the demo accounts as read. Use a development database. It stops with exit code 1 at the first failure.
set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
EMAIL_DOMAIN="${EMAIL_DOMAIN:-nst.rishihood.edu.in}"
DEMO_PASSWORD="${DEMO_PASSWORD:-Password@123}"
RACE_ROUNDS="${RACE_ROUNDS:-5}"
API="$BASE_URL/api"

for tool in curl jq; do
  command -v "$tool" >/dev/null || { echo "FAIL: $tool is required" >&2; exit 2; }
done

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

RUN_ID="$(date +%s)$$"
# The student email pattern allows letters only, so the run id becomes letters.
RUN_LETTERS="$(printf '%s' "$RUN_ID" | tr '0-9' 'a-j')"
SECTION=""
CHECKS=0
STATUS=""
BODY=""

section() { SECTION="$1"; printf '\n== %s\n' "$1"; }
pass() { CHECKS=$((CHECKS + 1)); printf '  ok   %s\n' "$1"; }

fail() {
  printf '  FAIL %s\n' "$1"
  printf '       section:  %s\n' "$SECTION"
  printf '       response: HTTP %s %s\n' "$STATUS" "$BODY"
  exit 1
}

# call METHOD PATH [TOKEN] [JSON]: sets STATUS and BODY.
call() {
  local method="$1" path="$2" token="${3:-}" json="${4:-}"
  local args=(-sS --max-time 40 -o "$TMP_DIR/body" -w '%{http_code}' -X "$method" -H 'Content-Type: application/json')
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$json" ] && args+=(-d "$json")
  STATUS="$(curl "${args[@]}" "$API$path")" || { STATUS="000"; BODY="(no response from $API$path)"; fail "request failed: $method $path"; }
  BODY="$(cat "$TMP_DIR/body")"
}

# expect DESCRIPTION HTTP_STATUS [ERROR_CODE]: checks the last call.
expect() {
  local description="$1" wanted="$2" code="${3:-}"
  [ "$STATUS" = "$wanted" ] || fail "$description: expected HTTP $wanted"
  if [ -n "$code" ]; then
    [ "$(jq -r '.code // empty' <<<"$BODY")" = "$code" ] || fail "$description: expected code $code"
  fi
  pass "$description (HTTP $wanted${code:+ $code})"
}

# holds DESCRIPTION [JQ_ARGS...] FILTER: the filter must be true for the last response.
holds() {
  local description="$1"; shift
  jq -e "$@" <<<"$BODY" >/dev/null || fail "$description"
  pass "$description"
}

# request DESCRIPTION WANTED CODE METHOD PATH [TOKEN] [JSON]
request() {
  local description="$1" wanted="$2" code="$3"; shift 3
  call "$@"
  expect "$description" "$wanted" "$code"
}

value() { jq -r "$1" <<<"$BODY"; }

complete_proposal() {
  jq -nc --arg t "${1:-8 weeks}" '{problemStatement:"Approvals on paper are slow",objectives:"Digitise the flow",methodology:"Spring Boot and React",expectedOutcome:"A working system",timeline:$t,referenceLink:null}'
}

# new_project TITLE TOKEN: creates a complete draft and prints its id.
new_project() {
  call POST /projects "$2" "$(jq -nc --arg t "$1 $RUN_ID" --argjson p "$(complete_proposal)" '{title:$t,description:"Created by the smoke test",proposal:$p}')"
  [ "$STATUS" = 201 ] || fail "create project $1: expected HTTP 201"
  value .id
}

# unread_of TOKEN: prints the unread count.
unread_of() {
  call GET /notifications/unread-count "$1"
  [ "$STATUS" = 200 ] || fail "read unread count: expected HTTP 200"
  value .count
}

# ------------------------------------------------------------------------------------------------------------------
section "Backend reachable"
curl -sS --max-time 10 -o /dev/null "$BASE_URL/v3/api-docs" || { echo "FAIL: no backend at $BASE_URL"; exit 1; }
pass "backend answers at $BASE_URL"

# ------------------------------------------------------------------------------------------------------------------
section "Registration rules and login"
register_body() { jq -nc --arg e "$1" --arg r "${2:-}" '{name:"Smoke Student",email:$e,password:"Password@123"} + (if $r == "" then {} else {role:$r} end)'; }
request "faculty-style email is rejected" 400 VALIDATION_FAILED POST /auth/register "" "$(register_body "smoke.teacher@$EMAIL_DOMAIN")"
request "another domain is rejected" 400 VALIDATION_FAILED POST /auth/register "" "$(register_body "smoke.25csai@example.org")"
request "another role is rejected" 400 VALIDATION_FAILED POST /auth/register "" "$(register_body "${RUN_LETTERS}.25csai@$EMAIL_DOMAIN" FACULTY)"
request "a blank name and short password give field errors" 400 VALIDATION_FAILED POST /auth/register "" '{"name":"","email":"nope","password":"x"}'
holds "field errors name the fields" '[.fieldErrors[].field] | contains(["name","email","password"])'
request "a valid student email registers" 201 "" POST /auth/register "" "$(register_body "${RUN_LETTERS}.25csai@$EMAIL_DOMAIN")"
holds "admission year and branch come from the email" '.user.admissionYear == 2025 and .user.branch == "CSAI" and .user.role == "STUDENT"'
request "a duplicate email is rejected" 400 VALIDATION_FAILED POST /auth/register "" "$(register_body "${RUN_LETTERS}.25csai@$EMAIL_DOMAIN")"
request "a wrong password is rejected" 401 UNAUTHENTICATED POST /auth/login "" "$(jq -nc --arg e "coordinator@$EMAIL_DOMAIN" '{email:$e,password:"wrong-password"}')"

call POST /auth/login "" "$(jq -nc --arg e "coordinator@$EMAIL_DOMAIN" --arg p "$DEMO_PASSWORD" '{email:$e,password:$p}')"
expect "coordinator logs in" 200; holds "coordinator has the COORDINATOR role" '.user.role == "COORDINATOR"'; COORDINATOR="$(value .token)"
call POST /auth/login "" "$(jq -nc --arg e "anita.rao@$EMAIL_DOMAIN" --arg p "$DEMO_PASSWORD" '{email:$e,password:$p}')"
expect "faculty logs in" 200; holds "faculty has the FACULTY role" '.user.role == "FACULTY"'; FACULTY="$(value .token)"; FACULTY_ID="$(value .user.id)"
call POST /auth/login "" "$(jq -nc --arg e "vikram.shah@$EMAIL_DOMAIN" --arg p "$DEMO_PASSWORD" '{email:$e,password:$p}')"
expect "second faculty logs in" 200; FACULTY2="$(value .token)"; FACULTY2_ID="$(value .user.id)"
call POST /auth/login "" "$(jq -nc --arg e "aarav.23csai@$EMAIL_DOMAIN" --arg p "$DEMO_PASSWORD" '{email:$e,password:$p}')"
expect "student logs in" 200; holds "student has the STUDENT role" '.user.role == "STUDENT"'; STUDENT="$(value .token)"; STUDENT_ID="$(value .user.id)"
call POST /auth/login "" "$(jq -nc --arg e "diya.23csai@$EMAIL_DOMAIN" --arg p "$DEMO_PASSWORD" '{email:$e,password:$p}')"
expect "another student logs in" 200; OTHER_STUDENT="$(value .token)"

# Start from an empty inbox so the counts below belong to this run.
call PATCH /notifications/read-all "$FACULTY"; call PATCH /notifications/read-all "$FACULTY2"; call PATCH /notifications/read-all "$STUDENT"

# ------------------------------------------------------------------------------------------------------------------
section "Student creates, edits and submits"
request "student creates a project with an incomplete draft" 201 "" POST /projects "$STUDENT" "$(jq -nc --arg t "Main flow $RUN_ID" '{title:$t,description:"Created by the smoke test",proposal:{problemStatement:"Only this is filled in"}}')"
holds "the project starts as DRAFT with version 1" '.status == "DRAFT" and .currentProposal.versionNumber == 1 and (.allowedActions | index("SUBMIT"))'
holds "missing fields are listed" '.currentProposal.missingFields | contains(["objectives","methodology","expectedOutcome","timeline"])'
PROJECT="$(value .id)"
request "an incomplete draft cannot be submitted" 400 VALIDATION_FAILED POST "/projects/$PROJECT/submit" "$STUDENT"
holds "the error names what is missing" '.message | contains("Missing")'
request "another student cannot edit the draft" 403 FORBIDDEN PUT "/projects/$PROJECT/proposal" "$OTHER_STUDENT" "$(complete_proposal)"
request "the owner completes the draft" 200 "" PUT "/projects/$PROJECT/proposal" "$STUDENT" "$(complete_proposal)"
holds "nothing is missing any more" '.missingFields == [] and .state == "DRAFT"'

# ------------------------------------------------------------------------------------------------------------------
section "Coordinator assigns a reviewer"
assign_body() { jq -nc --arg f "$1" '{facultyId:$f}'; }
request "assigning a DRAFT project is the wrong state" 409 ILLEGAL_TRANSITION POST "/projects/$PROJECT/assign" "$COORDINATOR" "$(assign_body "$FACULTY_ID")"
request "the owner submits" 200 "" POST "/projects/$PROJECT/submit" "$STUDENT"
holds "the project is SUBMITTED" '.status == "SUBMITTED"'
request "a submitted version cannot be edited" 409 ILLEGAL_TRANSITION PUT "/projects/$PROJECT/proposal" "$STUDENT" "$(complete_proposal)"
request "a student cannot assign" 403 FORBIDDEN POST "/projects/$PROJECT/assign" "$STUDENT" "$(assign_body "$FACULTY_ID")"
request "faculty cannot assign" 403 FORBIDDEN POST "/projects/$PROJECT/assign" "$FACULTY" "$(assign_body "$FACULTY_ID")"
request "the assignee must be faculty" 400 VALIDATION_FAILED POST "/projects/$PROJECT/assign" "$COORDINATOR" "$(assign_body "$STUDENT_ID")"
request "faculty cannot review before assignment" 409 ILLEGAL_TRANSITION POST "/projects/$PROJECT/reviews" "$FACULTY" '{"decision":"APPROVE"}'
request "the coordinator assigns faculty" 200 "" POST "/projects/$PROJECT/assign" "$COORDINATOR" "$(assign_body "$FACULTY_ID")"
holds "the project is UNDER_REVIEW with that reviewer" --arg f "$FACULTY_ID" '.status == "UNDER_REVIEW" and .reviewer.id == $f'
request "assigning again is the wrong state" 409 ILLEGAL_TRANSITION POST "/projects/$PROJECT/assign" "$COORDINATOR" "$(assign_body "$FACULTY2_ID")"

# ------------------------------------------------------------------------------------------------------------------
section "Revision loop and version comparison"
request "another faculty member cannot review" 403 FORBIDDEN POST "/projects/$PROJECT/reviews" "$FACULTY2" '{"decision":"APPROVE"}'
request "requesting a revision without comments is rejected" 400 VALIDATION_FAILED POST "/projects/$PROJECT/reviews" "$FACULTY" '{"decision":"REQUEST_REVISION"}'
request "faculty requests a revision with comments" 201 "" POST "/projects/$PROJECT/reviews" "$FACULTY" '{"decision":"REQUEST_REVISION","comments":"Clarify the methodology","versionNumber":1}'
request "the student reads the project" 200 "" GET "/projects/$PROJECT" "$STUDENT"
holds "the project is REVISION_REQUESTED and offers a new version" '.status == "REVISION_REQUESTED" and (.allowedActions | index("CREATE_REVISION"))'
request "the student reads the feedback" 200 "" GET "/projects/$PROJECT/reviews" "$STUDENT"
holds "the feedback points at version 1" '.[0].comments == "Clarify the methodology" and .[0].versionNumber == 1 and .[0].decision == "REQUEST_REVISION"'
request "resubmitting with no new version is rejected" 409 ILLEGAL_TRANSITION POST "/projects/$PROJECT/submit" "$STUDENT"
request "the student starts version 2" 201 "" POST "/projects/$PROJECT/proposals" "$STUDENT"
holds "version 2 is a draft prefilled from version 1" '.versionNumber == 2 and .state == "DRAFT" and .objectives == "Digitise the flow"'
request "a second open draft is rejected" 409 ILLEGAL_TRANSITION POST "/projects/$PROJECT/proposals" "$STUDENT"
request "the student edits version 2" 200 "" PUT "/projects/$PROJECT/proposal" "$STUDENT" "$(complete_proposal "10 weeks")"
request "the student resubmits" 200 "" POST "/projects/$PROJECT/submit" "$STUDENT"
holds "the project returns to the same reviewer" --arg f "$FACULTY_ID" '.status == "UNDER_REVIEW" and .reviewer.id == $f and .latestSubmittedVersion == 2'
request "versions 1 and 2 can be compared" 200 "" GET "/projects/$PROJECT/proposals/compare?from=1&to=2" "$STUDENT"
holds "only the timeline changed" '[.fields[] | select(.changed) | .name] == ["timeline"] and (.fields | length) == 6'
holds "the comparison shows both values" '.fields[] | select(.name == "timeline") | .fromValue == "8 weeks" and .toValue == "10 weeks"'
request "comparing a version with itself is rejected" 400 VALIDATION_FAILED GET "/projects/$PROJECT/proposals/compare?from=1&to=1" "$STUDENT"
OTHER_PROJECT="$(new_project "Other project" "$STUDENT")"
request "a version of another project is not found" 404 NOT_FOUND GET "/projects/$OTHER_PROJECT/proposals/compare?from=1&to=2" "$STUDENT"
request "version 1 is unchanged" 200 "" GET "/projects/$PROJECT/proposals" "$STUDENT"
holds "version 1 still holds the original timeline" '.[0].timeline == "8 weeks" and .[0].state == "SUBMITTED" and .[1].timeline == "10 weeks"'

# ------------------------------------------------------------------------------------------------------------------
section "Approval is final"
request "reviewing an old version is rejected" 409 ILLEGAL_TRANSITION POST "/projects/$PROJECT/reviews" "$FACULTY" '{"decision":"APPROVE","versionNumber":1}'
request "faculty approves version 2" 201 "" POST "/projects/$PROJECT/reviews" "$FACULTY" '{"decision":"APPROVE","versionNumber":2}'
holds "the review points at version 2" '.versionNumber == 2 and .decision == "APPROVE"'
request "the student reads the result" 200 "" GET "/projects/$PROJECT" "$STUDENT"
holds "the project is APPROVED with no actions left" '.status == "APPROVED" and .allowedActions == []'
request "approving again is illegal" 409 ILLEGAL_TRANSITION POST "/projects/$PROJECT/reviews" "$FACULTY" '{"decision":"APPROVE"}'
request "reassigning afterwards is illegal" 409 ILLEGAL_TRANSITION PUT "/projects/$PROJECT/assign" "$COORDINATOR" "$(assign_body "$FACULTY2_ID")"
request "the timeline records the whole flow" 200 "" GET "/projects/$PROJECT/history" "$STUDENT"
holds "the timeline holds versions, reviews and status changes" '[.[].type] | contains(["VERSION_CREATED","REVIEW","STATUS_CHANGED"])'

# ------------------------------------------------------------------------------------------------------------------
section "Notifications"
request "faculty lists unread notifications" 200 "" GET "/notifications?unread=true&size=50" "$FACULTY"
holds "faculty heard about the assignment and the resubmission" --arg p "$PROJECT" '[.content[] | select(.projectId == $p) | .type] | sort == ["RESUBMITTED","REVIEWER_ASSIGNED"]'
FIRST_NOTIFICATION="$(value '.content[0].id')"
request "the student lists unread notifications" 200 "" GET "/notifications?unread=true&size=50" "$STUDENT"
holds "the student heard both decisions" --arg p "$PROJECT" '[.content[] | select(.projectId == $p) | .type] == ["REVIEW_DECISION","REVIEW_DECISION"]'
BEFORE="$(unread_of "$FACULTY")"
[ "$BEFORE" -ge 2 ] || fail "faculty should have at least 2 unread notifications"; pass "faculty has $BEFORE unread"
request "another user cannot mark it read" 403 FORBIDDEN PATCH "/notifications/$FIRST_NOTIFICATION/read" "$STUDENT"
request "faculty marks one read" 200 "" PATCH "/notifications/$FIRST_NOTIFICATION/read" "$FACULTY"
holds "it is read" '.read == true'
AFTER="$(unread_of "$FACULTY")"
[ "$AFTER" -eq $((BEFORE - 1)) ] || fail "marking one read should lower the count from $BEFORE, got $AFTER"; pass "the unread count went from $BEFORE to $AFTER"
request "faculty marks all read" 200 "" PATCH /notifications/read-all "$FACULTY"
holds "mark all reports how many changed" --argjson n "$AFTER" '.updated == $n'
[ "$(unread_of "$FACULTY")" -eq 0 ] || fail "marking all read should leave 0 unread"; pass "the unread count is 0"

# ------------------------------------------------------------------------------------------------------------------
section "Reassignment"
SECOND="$(new_project "Reassigned" "$STUDENT")"
request "the second project is submitted" 200 "" POST "/projects/$SECOND/submit" "$STUDENT"
request "reassigning before assignment is illegal" 409 ILLEGAL_TRANSITION PUT "/projects/$SECOND/assign" "$COORDINATOR" "$(assign_body "$FACULTY2_ID")"
request "the second project goes to the first reviewer" 200 "" POST "/projects/$SECOND/assign" "$COORDINATOR" "$(assign_body "$FACULTY_ID")"
request "the first reviewer can view it" 200 "" GET "/projects/$SECOND" "$FACULTY"
request "a student cannot reassign" 403 FORBIDDEN PUT "/projects/$SECOND/assign" "$STUDENT" "$(assign_body "$FACULTY2_ID")"
request "reassigning to the current reviewer is rejected" 400 VALIDATION_FAILED PUT "/projects/$SECOND/assign" "$COORDINATOR" "$(assign_body "$FACULTY_ID")"
request "the coordinator reassigns" 200 "" PUT "/projects/$SECOND/assign" "$COORDINATOR" "$(assign_body "$FACULTY2_ID")"
holds "the status stays UNDER_REVIEW with the new reviewer" --arg f "$FACULTY2_ID" '.status == "UNDER_REVIEW" and .reviewer.id == $f'
request "the old reviewer's inbox" 200 "" GET "/notifications?unread=true&size=50" "$FACULTY"
holds "the old reviewer was told" --arg p "$SECOND" '[.content[] | select(.projectId == $p) | .type] | sort == ["REVIEWER_ASSIGNED","REVIEWER_UNASSIGNED"]'
request "the new reviewer's inbox" 200 "" GET "/notifications?unread=true&size=50" "$FACULTY2"
holds "the new reviewer was told" --arg p "$SECOND" '[.content[] | select(.projectId == $p) | .type] == ["REVIEWER_ASSIGNED"]'
request "the old reviewer can no longer view the project" 403 FORBIDDEN GET "/projects/$SECOND" "$FACULTY"
request "the old reviewer can no longer decide" 403 FORBIDDEN POST "/projects/$SECOND/reviews" "$FACULTY" '{"decision":"APPROVE"}'
request "the new reviewer can view the project" 200 "" GET "/projects/$SECOND" "$FACULTY2"

# ------------------------------------------------------------------------------------------------------------------
section "Audit log"
request "the coordinator reads the audit log of the first project" 200 "" GET "/audit?projectId=$PROJECT&size=100" "$COORDINATOR"
holds "every entry belongs to that project" --arg p "$PROJECT" '(.content | length) > 0 and all(.content[]; .projectId == $p)'
holds "the entries are newest first" '[.content[].createdAt] as $t | $t == ($t | sort | reverse)'
holds "the expected actions are there" '[.content[].action] | contains(["PROJECT_CREATED","VERSION_CREATED","DRAFT_UPDATED","PROPOSAL_SUBMITTED","REVIEWER_ASSIGNED","REVIEW_RECORDED","STATUS_CHANGED"])'
request "filter by project and action" 200 "" GET "/audit?projectId=$PROJECT&action=PROPOSAL_SUBMITTED" "$COORDINATOR"
holds "there are two submissions" '.totalElements == 2 and all(.content[]; .action == "PROPOSAL_SUBMITTED")'
request "the reassignment is audited" 200 "" GET "/audit?projectId=$SECOND&action=REVIEWER_REASSIGNED" "$COORDINATOR"
holds "there is one reassignment" '.totalElements == 1'
request "a reversed date range is rejected" 400 VALIDATION_FAILED GET "/audit?from=2026-11-01T00:00:00Z&to=2026-10-01T00:00:00Z" "$COORDINATOR"
request "a student cannot read the audit log" 403 FORBIDDEN GET /audit "$STUDENT"
request "faculty cannot read the audit log" 403 FORBIDDEN GET /audit "$FACULTY"

# ------------------------------------------------------------------------------------------------------------------
section "Dashboard against the project list"
request "the coordinator reads the dashboard" 200 "" GET /dashboard/summary "$COORDINATOR"
DASHBOARD="$BODY"
for status in DRAFT SUBMITTED UNDER_REVIEW REVISION_REQUESTED APPROVED REJECTED; do
  expected="$(jq -r --arg s "$status" '.byStatus[$s]' <<<"$DASHBOARD")"
  call GET "/projects?status=$status&size=1" "$COORDINATOR"
  [ "$(value .totalElements)" = "$expected" ] || fail "$status: dashboard says $expected, the list says $(value .totalElements)"
  pass "$status: dashboard and list both say $expected"
done
total="$(jq -r .total <<<"$DASHBOARD")"
call GET "/projects?size=1" "$COORDINATOR"
[ "$(value .totalElements)" = "$total" ] || fail "total: dashboard says $total, the list says $(value .totalElements)"; pass "total: dashboard and list both say $total"
[ "$(jq -r '.awaitingAssignment | length' <<<"$DASHBOARD")" = "$(jq -r .byStatus.SUBMITTED <<<"$DASHBOARD")" ] || fail "awaiting assignment should equal SUBMITTED"; pass "awaiting assignment equals SUBMITTED"
for id in "$FACULTY_ID" "$FACULTY2_ID"; do
  expected="$(jq -r --arg i "$id" '.reviewerWorkload[] | select(.reviewer.id == $i) | .openReviews' <<<"$DASHBOARD")"
  call GET "/projects?status=UNDER_REVIEW&reviewerId=$id&size=1" "$COORDINATOR"
  [ "$(value .totalElements)" = "$expected" ] || fail "workload: dashboard says $expected, the list says $(value .totalElements)"
  pass "workload of $id: dashboard and list both say $expected"
done
request "the summary endpoint agrees" 200 "" GET /projects/summary "$COORDINATOR"
holds "the summary total matches" --argjson t "$total" '.total == $t'
request "a student cannot read the dashboard" 403 FORBIDDEN GET /dashboard/summary "$STUDENT"

# ------------------------------------------------------------------------------------------------------------------
section "Access control"
for path in /auth/me /projects /notifications /audit /dashboard/summary "/projects/$PROJECT"; do
  request "no token on $path" 401 UNAUTHENTICATED GET "$path"
done
request "a bad token is rejected" 401 UNAUTHENTICATED GET /projects "not-a-real-token"
request "a student cannot create accounts" 403 FORBIDDEN POST /users "$STUDENT" "$(jq -nc --arg e "${RUN_LETTERS}@$EMAIL_DOMAIN" '{name:"X",email:$e,password:"Password@123",role:"FACULTY"}')"
request "faculty cannot create a project" 403 FORBIDDEN POST /projects "$FACULTY" '{"title":"x","description":"y"}'
request "a student cannot see another student's project" 403 FORBIDDEN GET "/projects/$PROJECT" "$OTHER_STUDENT"
request "nor its reviews" 403 FORBIDDEN GET "/projects/$PROJECT/reviews" "$OTHER_STUDENT"
request "nor its versions" 403 FORBIDDEN GET "/projects/$PROJECT/proposals" "$OTHER_STUDENT"
request "nor its timeline" 403 FORBIDDEN GET "/projects/$PROJECT/history" "$OTHER_STUDENT"
request "nor its comparison" 403 FORBIDDEN GET "/projects/$PROJECT/proposals/compare?from=1&to=2" "$OTHER_STUDENT"
request "that student's list does not show it" 200 "" GET "/projects?q=$RUN_ID&size=50" "$OTHER_STUDENT"
holds "the list is empty for that student" '.totalElements == 0'
request "a student cannot filter by cohort" 403 FORBIDDEN GET "/projects?branch=CSAI" "$STUDENT"
request "an unknown project is not found" 404 NOT_FOUND GET /projects/00000000-0000-0000-0000-000000000000 "$COORDINATOR"
request "a malformed id is rejected" 400 VALIDATION_FAILED GET /projects/not-a-uuid "$COORDINATOR"

# ------------------------------------------------------------------------------------------------------------------
section "Race: two simultaneous submits"
submit_in_background() {
  curl -sS --max-time 40 -o "$TMP_DIR/race$1.body" -w '%{http_code}' -X POST -H "Authorization: Bearer $STUDENT" "$API/projects/$2/submit" >"$TMP_DIR/race$1.status" &
}
round=1
while [ "$round" -le "$RACE_ROUNDS" ]; do
  RACE="$(new_project "Race $round" "$STUDENT")"
  submit_in_background 1 "$RACE"
  submit_in_background 2 "$RACE"
  wait
  STATUS="$(cat "$TMP_DIR/race1.status" "$TMP_DIR/race2.status" | tr '\n' ' ')"
  BODY="$(cat "$TMP_DIR/race1.body" "$TMP_DIR/race2.body")"
  codes="$(printf '%s %s\n' "$(cat "$TMP_DIR/race1.status")" "$(cat "$TMP_DIR/race2.status")" | tr ' ' '\n' | sort | tr '\n' ' ')"
  [ "$codes" = "200 409 " ] || fail "round $round: expected exactly one 200 and one 409, got $codes"
  loser=1; [ "$(cat "$TMP_DIR/race1.status")" = 409 ] || loser=2
  code="$(jq -r .code "$TMP_DIR/race$loser.body")"
  case "$code" in
    CONCURRENT_UPDATE|ILLEGAL_TRANSITION) ;;
    *) fail "round $round: the loser's code is $code" ;;
  esac
  request "round $round: one 200, one 409 $code, and the project is SUBMITTED" 200 "" GET "/projects/$RACE" "$STUDENT"
  holds "round $round: the project is SUBMITTED" '.status == "SUBMITTED"'
  request "round $round: audit" 200 "" GET "/audit?projectId=$RACE&action=PROPOSAL_SUBMITTED" "$COORDINATOR"
  holds "round $round: exactly one submission is audited" '.totalElements == 1'
  round=$((round + 1))
done

printf '\nPASS: %s checks in run %s\n' "$CHECKS" "$RUN_ID"
