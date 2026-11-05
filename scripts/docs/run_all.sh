#!/usr/bin/env bash
# Runs every documentation check. Exit code 1 if any of them finds a problem.
cd "$(dirname "$0")" || exit 2
status=0
python3 check_names.py || status=1
python3 check_diagrams.py || status=1
python3 check_refs.py || status=1
exit "$status"
