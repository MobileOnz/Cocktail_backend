#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
export PYTHONDONTWRITEBYTECODE=1
python3 -m unittest discover -s scripts/hybrid_search -p 'test_*.py' -v
python3 scripts/hybrid_search/normalize.py data/hybrid-search/onz_cocktails_105.csv
if [[ -z "${SEARCH_TEST_JDBC_URL:-}" ]]; then
  echo 'SEARCH_TEST_JDBC_URL is required for PostgreSQL integration checks.' >&2
  exit 1
fi
./gradlew test --rerun-tasks --console=plain
