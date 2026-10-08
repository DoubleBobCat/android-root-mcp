#!/usr/bin/env bash
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$project_root/src"

if [[ "${1:-}" == "test" && $# -eq 1 ]]; then
  set -- test ../test
fi

exec flutter "$@"
