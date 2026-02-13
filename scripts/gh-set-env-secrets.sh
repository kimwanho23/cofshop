#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 2 || $# -gt 3 ]]; then
  echo "Usage: $0 <environment> <env-file> [ssh-key-file]"
  echo "Example: $0 development .secrets/development.env ~/.ssh/ec2_key.pem"
  exit 1
fi

ENV_NAME="$1"
ENV_FILE="$2"
SSH_KEY_FILE="${3:-}"

if ! command -v gh >/dev/null 2>&1; then
  echo "gh CLI is required. Install: https://cli.github.com/"
  exit 1
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Env file not found: $ENV_FILE"
  exit 1
fi

if [[ -n "$SSH_KEY_FILE" && ! -f "$SSH_KEY_FILE" ]]; then
  echo "SSH key file not found: $SSH_KEY_FILE"
  exit 1
fi

echo "Setting secrets for environment: ${ENV_NAME}"

set_count=0
skip_count=0

while IFS= read -r raw_line || [[ -n "$raw_line" ]]; do
  line="$(echo "$raw_line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"

  if [[ -z "$line" || "$line" == \#* ]]; then
    continue
  fi

  if [[ "$line" != *=* ]]; then
    echo "Skipping invalid line: $line"
    continue
  fi

  key="${line%%=*}"
  value="${line#*=}"

  key="$(echo "$key" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"

  # Remove optional surrounding quotes in value.
  if [[ "$value" =~ ^\".*\"$ ]]; then
    value="${value:1:${#value}-2}"
  elif [[ "$value" =~ ^\'.*\'$ ]]; then
    value="${value:1:${#value}-2}"
  fi

  if [[ -z "$key" ]]; then
    continue
  fi

  # Prevent accidental empty-secret uploads from template files.
  if [[ -z "$value" ]]; then
    echo "  - skip $key (empty value)"
    skip_count=$((skip_count + 1))
    continue
  fi

  # Force gh to not read from this loop's stdin.
  gh secret set "$key" --env "$ENV_NAME" --body "$value" >/dev/null </dev/null
  echo "  - set $key"
  set_count=$((set_count + 1))
done < "$ENV_FILE"

if [[ -n "$SSH_KEY_FILE" ]]; then
  gh secret set EC2_SSH_KEY --env "$ENV_NAME" < "$SSH_KEY_FILE"
  echo "  - set EC2_SSH_KEY (from file)"
  set_count=$((set_count + 1))
fi

echo "Done. set=${set_count}, skipped=${skip_count}"
