#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
phone_serial="${PHONE_SERIAL:-emulator-5554}"
adb_bin="${ANDROID_HOME:?ANDROID_HOME must point to the Android SDK}/platform-tools/adb"
test_class="com.motionapps.sensorbox.emulator.PhoneSensorRecordingEmulatorTest"
runner="motionapps.sensorbox.test/androidx.test.runner.AndroidJUnitRunner"
result_file="$(mktemp -t sensorbox-phone-test.XXXXXX)"

cleanup() {
    rm -f "$result_file"
}
trap cleanup EXIT

cd "$repo_root"
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
"$adb_bin" -s "$phone_serial" install -r app/build/outputs/apk/debug/app-debug.apk
"$adb_bin" -s "$phone_serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
"$adb_bin" -s "$phone_serial" shell pm grant motionapps.sensorbox android.permission.POST_NOTIFICATIONS || true
"$adb_bin" -s "$phone_serial" logcat -c

"$adb_bin" -s "$phone_serial" shell am instrument -w -r -e class "$test_class" "$runner" >"$result_file" &
test_pid=$!

sleep 3
for value in "1.25:2.5:9.5" "-3.0:4.25:8.75" "6.5:-1.5:7.25"; do
    "$adb_bin" -s "$phone_serial" emu sensor set acceleration "$value"
    sleep 1
done

wait "$test_pid"
cat "$result_file"
grep -q "OK (1 test)" "$result_file"
