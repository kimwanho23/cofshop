#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RESOURCE_DIR="$ROOT_DIR/src/main/resources"

FILES=(
  "application.properties"
  "application-dev.properties"
  "application-prod.properties"
  "application-key.properties"
)

for file_name in "${FILES[@]}"; do
  template_file="${RESOURCE_DIR}/${file_name}.template"
  target_file="${RESOURCE_DIR}/${file_name}"

  if [[ ! -f "$template_file" ]]; then
    echo "template missing: $template_file" >&2
    exit 1
  fi

  if [[ -f "$target_file" ]]; then
    echo "skip existing: $target_file"
    continue
  fi

  cp "$template_file" "$target_file"
  echo "created: $target_file"
done
