#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
adb_bin="${ADB_BIN:-$(command -v adb)}"
phone_test="com.tomasrepcik.sensorbox.pairedrecording.PhonePairedRecordingEmulatorTest"
runner="com.tomasrepcik.sensorbox.test/androidx.test.runner.AndroidJUnitRunner"
result_file="$(mktemp -t sensorbox-paired-recording.XXXXXX)"

cleanup() {
    rm -f "$result_file"
}
trap cleanup EXIT

detect_emulator() {
    local expected_role="$1"
    local serial characteristics
    local matches=()

    while read -r serial; do
        [[ "$serial" == emulator-* ]] || continue
        characteristics="$($adb_bin -s "$serial" shell getprop ro.build.characteristics </dev/null | tr -d '\r')"
        if [[ "$expected_role" == "wear" ]]; then
            if [[ "$characteristics" == *watch* ]]; then
                matches+=("$serial")
            fi
        elif [[ "$characteristics" != *watch* ]]; then
            matches+=("$serial")
        fi
        :
    done < <("$adb_bin" devices | awk 'NR > 1 && $2 == "device" { print $1 }')

    if [[ "${#matches[@]}" -ne 1 ]]; then
        local serial_variable="PHONE_SERIAL"
        [[ "$expected_role" == "wear" ]] && serial_variable="WEAR_SERIAL"
        echo "Expected exactly one $expected_role emulator; found: ${matches[*]:-none}. Set $serial_variable." >&2
        exit 2
    fi
    printf '%s' "${matches[0]}"
}

phone_serial="${PHONE_SERIAL:-$(detect_emulator phone)}"
wear_serial="${WEAR_SERIAL:-$(detect_emulator wear)}"

connect_emulators() {
    "$adb_bin" -s "$phone_serial" forward tcp:5602 tcp:5601 >/dev/null
    "$adb_bin" -s "$wear_serial" reverse tcp:5601 tcp:5602 >/dev/null
    "$adb_bin" -s "$wear_serial" shell am broadcast \
        -a com.google.android.gms.wearable.EMULATOR \
        --es operation refresh-emulator-connection >/dev/null
}

require_paired_emulators() {
    local pairing_status
    pairing_status="$("$adb_bin" -s "$wear_serial" shell am broadcast \
        -a com.google.android.gms.wearable.EMULATOR \
        --es operation get-pairing-status)"
    if [[ "$pairing_status" == *"Peer:[null"* ]]; then
        echo "Wear emulator has no paired phone. Pair both devices with Android Studio's Pairing Assistant first." >&2
        exit 2
    fi
}

cd "$repo_root"
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest :wear:assembleDebug

"$adb_bin" -s "$phone_serial" install -r app/build/outputs/apk/debug/app-debug.apk
"$adb_bin" -s "$phone_serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
"$adb_bin" -s "$wear_serial" install -r wear/build/outputs/apk/debug/wear-debug.apk

"$adb_bin" -s "$phone_serial" shell pm clear com.tomasrepcik.sensorbox >/dev/null
"$adb_bin" -s "$wear_serial" shell pm clear com.tomasrepcik.sensorbox >/dev/null
connect_emulators
require_paired_emulators

echo "Running phone-driven paired recording on $phone_serial with $wear_serial"
"$adb_bin" -s "$phone_serial" shell am instrument -w -r \
    -e class "$phone_test" "$runner" >"$result_file"
cat "$result_file"
grep -q "OK (1 test)" "$result_file"
