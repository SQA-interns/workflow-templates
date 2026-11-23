#!/usr/bin/env bash
# Hidden suite (general/testing/hidden-evaluation.md). Runs from the project root:
#   bash 02_output/hidden-tests/run.sh
# Copies the backend into a temporary folder, adds the hidden tests there (package
# app.contacts.hidden), runs them with the backend's own build (Testcontainers: PostgreSQL and the
# Firebase Auth emulator image of 02_output/docker/firebase-emulator) and writes one JUnit report:
#   02_output/hidden-tests/target/hidden-junit.xml
# The project tree is never changed. Exit code: 0 when every hidden test passed, else 1.
set -uo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
output="$(cd "$here/.." && pwd)"
target="$here/target"
rm -rf "$target"
mkdir -p "$target"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

cp -r "$output/backend" "$work/backend"
rm -rf "$work/backend/target"
mkdir -p "$work/docker"
cp -r "$output/docker/firebase-emulator" "$work/docker/firebase-emulator"
mkdir -p "$work/backend/src/test/java/app/contacts/hidden"
cp -r "$here/src/hidden/." "$work/backend/src/test/java/app/contacts/hidden/"

(
  cd "$work/backend" &&
    ./mvnw -B verify \
      -Dit.test='app/contacts/hidden/**/*IT' \
      -Dtest='app/contacts/hidden/**/*Test' \
      -Dsurefire.failIfNoSpecifiedTests=false \
      -Dfailsafe.failIfNoSpecifiedTests=false \
      -Djacoco.skip=true \
      -Demulator.dockerfile.dir="$work/docker/firebase-emulator"
) > "$target/maven.log" 2>&1
python3 "$here/merge-junit.py" "$work/backend/target/failsafe-reports" "$target/hidden-junit.xml"
