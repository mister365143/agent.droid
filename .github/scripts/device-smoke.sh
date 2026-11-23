#!/usr/bin/env bash
#
# Device start-up smoke test, run against an already-booted emulator or device.
#
# Installs the debug APK, launches MainActivity and asserts the three things a
# start-up crash would break: the app process exists, MainActivity is the
# resumed activity, and the crash buffer stayed clean. Run from the repo root;
# an optional first argument overrides the APK path.
set -euo pipefail

PACKAGE="dev.agentdroid.app"
ACTIVITY="$PACKAGE/.MainActivity"
APK="${1:-app/build/outputs/apk/debug/app-debug.apk}"

fail() {
  echo "::error::$*"
  exit 1
}

step() {
  echo
  echo "=== $* ==="
}

if [[ ! -f "$APK" ]]; then
  fail "debug APK not found at $APK — build it with ./gradlew assembleDebug"
fi

# An empty crash buffer before launch means anything in it afterwards happened
# during this run.
adb logcat -b crash -c || true

step "Install $APK"
adb install -r -t "$APK" || fail "adb install failed for $APK"

step "Launch $ACTIVITY"
# -W waits for the launch to be reported, so a hang or an immediate crash shows
# up here instead of as a flaky later assertion.
adb shell am start -W -n "$ACTIVITY" || fail "am start failed for $ACTIVITY"

step "Assertion 1/3: app process is running"
pid=""
for _ in $(seq 1 15); do
  pid="$(adb shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r\n ')"
  [[ -n "$pid" ]] && break
  sleep 1
done
[[ -n "$pid" ]] || fail "$PACKAGE has no live process after launch (it started and died)"
echo "OK: $PACKAGE is running (pid $pid)"

step "Assertion 2/3: MainActivity is the resumed activity"
resumed=""
for _ in $(seq 1 15); do
  resumed="$(adb shell dumpsys activity activities 2>/dev/null \
    | tr -d '\r' \
    | grep -E 'mResumedActivity|topResumedActivity' \
    | grep -F "$ACTIVITY" || true)"
  [[ -n "$resumed" ]] && break
  sleep 1
done
if [[ -z "$resumed" ]]; then
  echo "--- current resumed activity ---"
  adb shell dumpsys activity activities 2>/dev/null | tr -d '\r' \
    | grep -E 'ResumedActivity|mFocusedApp' || true
  fail "$ACTIVITY is not the resumed activity — the app launched but did not come to the foreground"
fi
echo "OK: ${resumed#"${resumed%%[![:space:]]*}"}"

step "Assertion 3/3: crash buffer is clean"
crashes="$(adb logcat -b crash -d 2>/dev/null | tr -d '\r' || true)"
if grep -q 'FATAL EXCEPTION' <<<"$crashes" && grep -q "$PACKAGE" <<<"$crashes"; then
  echo "--- crash buffer ---"
  echo "$crashes"
  fail "$PACKAGE crashed during start-up"
fi
if grep -q 'FATAL EXCEPTION' <<<"$crashes"; then
  # Not ours, but worth seeing in the log; a foreign crash does not fail the run.
  echo "Note: crash buffer contains a crash from another process:"
  echo "$crashes"
fi
echo "OK: no crash recorded for $PACKAGE"

step "Smoke test passed"
