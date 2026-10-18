#!/usr/bin/env sh
# Bootstrap wrapper: uses Gradle 8.13 from PATH, or downloads the pinned distribution.
set -eu
VERSION="8.13"
if command -v gradle >/dev/null 2>&1 && gradle --version 2>/dev/null | grep -q "Gradle ${VERSION}"; then
  exec gradle "$@"
fi
BASE="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/gradle-${VERSION}-bin/blockverse-bootstrap"
GRADLE_HOME="$BASE/gradle-${VERSION}"
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  mkdir -p "$BASE"
  ZIP="$BASE/gradle-${VERSION}-bin.zip"
  echo "Gradle ${VERSION} tidak ada di PATH; mengunduh distribusi resmi..."
  if command -v curl >/dev/null 2>&1; then
    curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-${VERSION}-bin.zip" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP" "https://services.gradle.org/distributions/gradle-${VERSION}-bin.zip"
  else
    echo "Install curl atau wget untuk mengunduh Gradle ${VERSION}." >&2; exit 1
  fi
  if command -v unzip >/dev/null 2>&1; then unzip -oq "$ZIP" -d "$BASE"
  else echo "Install unzip untuk mengekstrak Gradle." >&2; exit 1; fi
fi
exec "$GRADLE_HOME/bin/gradle" "$@"
