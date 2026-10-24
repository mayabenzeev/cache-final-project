#!/usr/bin/env bash
# Calls a URL a few times and prints how long each call took.
#
#   ./scripts/time-it.sh /ex1/plans/basic
#   ./scripts/time-it.sh /ex1/annotated/plans/basic 5
#
# Windows (PowerShell) equivalent for one call:
#   curl.exe -s -o NUL -w "%{time_total}s`n" http://localhost:8080/ex1/plans/basic

path="${1:-/ex1/plans/basic}"
count="${2:-3}"

for i in $(seq 1 "$count"); do
  curl -s -o /dev/null -w "call $i: %{http_code}  %{time_total}s\n" "http://localhost:8080${path}"
done
