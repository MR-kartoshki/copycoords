#!/usr/bin/env bash
set -euo pipefail

VERSIONS=()

add_version() {
  local v="$1"
  [[ -n "$v" ]] || return 0
  VERSIONS+=("$v")
}

collect_from_stonecutter_all_versions() {
  local file="stonecutter.gradle"
  [[ -f "$file" ]] || return 0

  # Parse the allVersions list in stonecutter.gradle.
  while IFS= read -r v; do
    add_version "$v"
  done < <(
    awk '
      BEGIN { in_list = 0 }
      {
        if (!in_list && $0 ~ /^[[:space:]]*def[[:space:]]+allVersions[[:space:]]*=[[:space:]]*\[/) {
          in_list = 1
        }
        if (in_list) {
          while (match($0, /\047[^\047]+\047|"[^"]+"/)) {
            token = substr($0, RSTART, RLENGTH)
            gsub(/^\047|\047$/, "", token)
            gsub(/^"|"$/, "", token)
            print token
            $0 = substr($0, RSTART + RLENGTH)
          }
          if ($0 ~ /\]/) {
            in_list = 0
            exit
          }
        }
      }
    ' "$file"
  )
}

collect_from_built_artifacts() {
  [[ -d versions ]] || return 0

  while IFS= read -r v; do
    add_version "$v"
  done < <(
    find versions -mindepth 4 -maxdepth 4 -type d -path 'versions/*/build/libs' -printf '%h\n' \
      | awk -F/ '{print $2}' \
      | sort -uV
  )
}

collect_from_version_dirs() {
  [[ -d versions ]] || return 0

  while IFS= read -r v; do
    add_version "$v"
  done < <(find versions -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | sort -uV)
}

collect_from_stonecutter_all_versions

if [[ ${#VERSIONS[@]} -eq 0 ]]; then
  collect_from_built_artifacts
fi

if [[ ${#VERSIONS[@]} -eq 0 ]]; then
  collect_from_version_dirs
fi

if [[ ${#VERSIONS[@]} -eq 0 ]]; then
  echo "::error::Unable to discover built versions from stonecutter.gradle or versions/."
  exit 1
fi

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  echo "discovered_versions=${VERSIONS[*]}" >> "$GITHUB_OUTPUT"
fi

printf 'Discovered built versions: %s\n' "${VERSIONS[*]}"
