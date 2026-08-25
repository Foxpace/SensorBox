#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
phone_serial="${PHONE_SERIAL:-emulator-5554}"
wear_serial="${WEAR_SERIAL:-emulator-5556}"
adb_bin="${ANDROID_HOME:?ANDROID_HOME must point to the Android SDK}/platform-tools/adb"
phone_test="com.tomasrepcik.sensorbox.emulator.WearToPhoneSyncReceiverEmulatorTest"
wear_test="com.tomasrepcik.sensorbox.emulator.WearToPhoneSyncSenderEmulatorTest"
runner="com.tomasrepcik.sensorbox.test/androidx.test.runner.AndroidJUnitRunner"
result_directory="$(mktemp -d -t sensorbox-wear-sync.XXXXXX)"
phone_result="$result_directory/phone.txt"
wear_result="$result_directory/wear.txt"

cleanup() {
    rm -rf "$result_directory"
}
trap cleanup EXIT

install_apks() {
    "$adb_bin" -s "$phone_serial" install -r app/build/outputs/apk/debug/app-debug.apk
    "$adb_bin" -s "$phone_serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
    "$adb_bin" -s "$wear_serial" install -r wear/build/outputs/apk/debug/wear-debug.apk
    "$adb_bin" -s "$wear_serial" install -r wear/build/outputs/apk/androidTest/debug/wear-debug-androidTest.apk
}

connect_emulators() {
    "$adb_bin" -s "$phone_serial" forward tcp:5602 tcp:5601
    "$adb_bin" -s "$wear_serial" reverse tcp:5601 tcp:5602
    "$adb_bin" -s "$wear_serial" shell am broadcast \
        -a com.google.android.gms.wearable.EMULATOR \
        --es operation refresh-emulator-connection >/dev/null
}

require_paired_emulators() {
    pairing_status="$("$adb_bin" -s "$wear_serial" shell am broadcast \
        -a com.google.android.gms.wearable.EMULATOR \
        --es operation get-pairing-status)"
    if [[ "$pairing_status" == *"Peer:[null"* ]]; then
        echo "Wear emulator has no paired phone. Pair both devices with Android Studio's Pairing Assistant first." >&2
        exit 2
    fi
}

run_receiver() {
    "$adb_bin" -s "$phone_serial" shell am instrument -w -r \
        -e class "$phone_test" "$runner" >"$phone_result" &
    phone_test_pid=$!
}

cd "$repo_root"
connect_emulators
require_paired_emulators
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest \
    :wear:assembleDebug :wear:assembleDebugAndroidTest
install_apks
run_receiver
sleep 2

"$adb_bin" -s "$wear_serial" shell am instrument -w -r \
    -e class "$wear_test" "$runner" >"$wear_result"
wait "$phone_test_pid"

cat "$wear_result"
cat "$phone_result"
grep -q "OK (1 test)" "$wear_result"
grep -q "OK (1 test)" "$phone_result"
