#!/usr/bin/env sh
set -eu
ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac -encoding UTF-8 -d "$OUT" \
  "$ROOT/core/src/main/java/com/blockverse/game/world/BlockType.java" \
  "$ROOT/core/src/main/java/com/blockverse/game/world/BlockPos.java" \
  "$ROOT/core/src/main/java/com/blockverse/game/world/BlockCell.java" \
  "$ROOT/core/src/main/java/com/blockverse/game/world/BlockWorld.java" \
  "$ROOT/core/src/main/java/com/blockverse/game/player/PlayerController.java" \
  "$ROOT/scripts/SmokeTest.java"
java -cp "$OUT" SmokeTest
