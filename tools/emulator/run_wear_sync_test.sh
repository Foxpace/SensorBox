#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
adb_bin="${ANDROID_HOME:?ANDROID_HOME must point to the Android SDK}/platform-tools/adb"
phone_test="com.tomasrepcik.sensorbox.measurements.sync.WearToPhoneSyncReceiverEmulatorTest"
wear_test="com.tomasrepcik.sensorbox.sync.WearToPhoneSyncSenderEmulatorTest"
runner="com.tomasrepcik.sensorbox.test/androidx.test.runner.AndroidJUnitRunner"
result_directory="$(mktemp -d -t sensorbox-wear-sync.XXXXXX)"
scenarios=(
    single_csv
    mixed_formats
    empty_and_ignored
    overwrite_existing
    same_name_different_sessions
    large_payload
)
phone_test_pid=""

cleanup() {
    if [[ -n "$phone_test_pid" ]]; then
        kill "$phone_test_pid" 2>/dev/null || true
    fi
    rm -rf "$result_directory"
}
trap cleanup EXIT

detect_device() {
    local expected_role="$1"
    local serial characteristics
    local matches=()

    while read -r serial; do
        characteristics="$($adb_bin -s "$serial" shell getprop ro.build.characteristics </dev/null | tr -d '\r')"
        if [[ "$expected_role" == "wear" ]]; then
            if [[ "$characteristics" == *watch* ]]; then
                matches+=("$serial")
            fi
        elif [[ "$characteristics" != *watch* ]]; then
            matches+=("$serial")
        fi
        : # Keep `set -e` from ending the loop when this device is the other form factor.
    done < <("$adb_bin" devices | awk 'NR > 1 && $2 == "device" { print $1 }')

    if [[ "${#matches[@]}" -ne 1 ]]; then
        local serial_variable="PHONE_SERIAL"
        [[ "$expected_role" == "wear" ]] && serial_variable="WEAR_SERIAL"
        echo "Expected exactly one $expected_role emulator; found: ${matches[*]:-none}. Set $serial_variable." >&2
        exit 2
    fi
    printf '%s' "${matches[0]}"
}

phone_serial="${PHONE_SERIAL:-$(detect_device phone)}"
wear_serial="${WEAR_SERIAL:-$(detect_device wear)}"

install_apks() {
    "$adb_bin" -s "$phone_serial" install -r app/build/outputs/apk/debug/app-debug.apk
    "$adb_bin" -s "$phone_serial" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
    "$adb_bin" -s "$wear_serial" install -r wear/build/outputs/apk/debug/wear-debug.apk
    "$adb_bin" -s "$wear_serial" install -r wear/build/outputs/apk/androidTest/debug/wear-debug-androidTest.apk
}

connect_emulators() {
    "$adb_bin" -s "$phone_serial" forward tcp:5602 tcp:5601 >/dev/null
    "$adb_bin" -s "$wear_serial" reverse tcp:5601 tcp:5602 >/dev/null
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

run_scenario() {
    local scenario="$1"
    local phone_result="$result_directory/$scenario-phone.txt"
    local wear_result="$result_directory/$scenario-wear.txt"

    echo "Running paired sync scenario: $scenario"
    "$adb_bin" -s "$phone_serial" shell pm clear com.tomasrepcik.sensorbox >/dev/null
    "$adb_bin" -s "$wear_serial" shell pm clear com.tomasrepcik.sensorbox >/dev/null
    "$adb_bin" -s "$phone_serial" shell am instrument -w -r \
        -e syncScenario "$scenario" -e class "$phone_test" "$runner" >"$phone_result" &
    phone_test_pid=$!
    sleep 2

    "$adb_bin" -s "$wear_serial" shell am instrument -w -r \
        -e syncScenario "$scenario" -e class "$wear_test" "$runner" >"$wear_result"
    wait "$phone_test_pid"
    phone_test_pid=""

    cat "$wear_result"
    cat "$phone_result"
    grep -q "OK (1 test)" "$wear_result"
    grep -q "OK (1 test)" "$phone_result"
}

cd "$repo_root"
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest \
    :wear:assembleDebug :wear:assembleDebugAndroidTest
install_apks
connect_emulators
require_paired_emulators
for scenario in "${scenarios[@]}"; do
    run_scenario "$scenario"
done
