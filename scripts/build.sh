#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./scripts/fetch-deps.sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug "$@"
