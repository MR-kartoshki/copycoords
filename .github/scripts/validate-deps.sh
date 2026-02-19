#!/usr/bin/env bash
set -euo pipefail

DEPS_FILE="${1:-deps.json}"

if [[ ! -f "$DEPS_FILE" ]]; then
  echo "No $DEPS_FILE found, skipping validation"
  exit 0
fi

jq -e '
  if type != "array" then
    error("deps.json must be a JSON array")
  else
    map(
      if type != "object" then
        error("Each dependency entry must be a JSON object")
      elif (has("project_id") and has("dependency_type")) then
        .
      else
        error("Each dependency must include project_id and dependency_type")
      end
      | if ((.project_id | type) == "string" and (.project_id | length) > 0) then .
        else error("project_id must be a non-empty string") end
      | if ((.dependency_type | type) == "string" and (["required","optional","incompatible","embedded"] | index(.dependency_type)) != null) then .
        else error("dependency_type must be one of: required, optional, incompatible, embedded") end
    )
    | true
  end
' "$DEPS_FILE" > /dev/null

echo "$DEPS_FILE validation passed"