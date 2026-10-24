#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p .deps
fetch() {
  local name="$1" url="$2" revision="$3"
  if [ ! -d ".deps/$name/.git" ]; then
    if [ -f ".deps/$name/.cadence-vendored-revision" ] && [ "$(cat ".deps/$name/.cadence-vendored-revision")" = "$revision" ]; then
      return 0
    fi
    if [ -e ".deps/$name" ]; then
      echo "Refusing to overwrite existing .deps/$name; inspect it before fetching." >&2
      exit 1
    fi
    git clone --no-checkout "$url" ".deps/$name"
  fi
  if [ "$(git -C ".deps/$name" rev-parse HEAD 2>/dev/null || true)" != "$revision" ]; then
    git -C ".deps/$name" fetch origin "$revision"
    git -C ".deps/$name" checkout --detach "$revision"
  fi
}
fetch NewPipeExtractor https://github.com/TeamNewPipe/NewPipeExtractor.git 711f13b15be314286a0e0a019302ad540c8f0ed7
fetch nanojson https://github.com/TeamNewPipe/nanojson.git e9d656ddb49a412a5a0a5d5ef20ca7ef09549996
