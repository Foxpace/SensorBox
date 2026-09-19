#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_NAME="$(basename -- "$0")"
SCRIPT_DIRECTORY="$(cd -- "$(dirname -- "$0")" && pwd -P)"
PACKAGE_NAME="com.tomasrepcik.sensorbox"
VERSION_NAME=""
PHONE_VERSION_CODE=""
WEAR_VERSION_CODE=""
PLAY_TRACK="internal"
WEAR_PLAY_TRACK=""
RELEASE_STATUS="draft"
OUTPUT_DIRECTORY="build/play-release"
VALIDATE_ONLY=false
BUILD_ONLY=false

PHONE_BUILD_FLOOR=87
WEAR_BUILD_FLOOR=1000049
BUILD_EPOCH=1789776000 # 2026-09-19 00:00:00 UTC

generate_version_name() {
  date -u '+%Y.%m.%d.%H'
}

generate_version_step() {
  local now_utc

  now_utc="$(date -u '+%s')"
  printf '%s\n' "$((1 + (now_utc - BUILD_EPOCH) / 60))"
}

usage() {
  cat <<EOF
Build and sign SensorBox phone and Wear OS Android releases.

Usage:
  $SCRIPT_NAME \
    [--package-name com.example.app] \
    [--version-name 2026.09.19.12] \
    [--phone-version-code 88] \
    [--wear-version-code 1000050] \
    [--track internal] \
    [--wear-track wear:qa] \
    [--release-status draft] \
    [--output-directory PATH] \
    [--build-only] \
    [--validate-only]

Required signing environment variables:
  SENSORBOX_ANDROID_KEY_ALIAS
  SENSORBOX_ANDROID_KEYSTORE_BASE64
  SENSORBOX_ANDROID_KEY_PASSWORD
  SENSORBOX_ANDROID_KEYSTORE_PASSWORD

Required only when uploading:
  GOOGLE_PLAY_SERVICE_ACCOUNT_BASE64

Use --build-only to create signed AAB and APK files without contacting Google Play.
EOF
}

die() {
  printf 'Error: %s\n' "$*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || die "Required command not found: $1"
}

require_environment_variable() {
  [[ -n "${!1:-}" ]] || die "Required environment variable is missing: $1"
}

find_apksigner() {
  local sdk_directory

  if command -v apksigner >/dev/null 2>&1; then
    command -v apksigner
    return
  fi

  sdk_directory="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  if [[ -z "$sdk_directory" && -f "$SCRIPT_DIRECTORY/local.properties" ]]; then
    sdk_directory="$(sed -n 's/^sdk\.dir=//p' "$SCRIPT_DIRECTORY/local.properties" | head -n 1)"
  fi

  [[ -n "$sdk_directory" && -d "$sdk_directory/build-tools" ]] || return 1
  find "$sdk_directory/build-tools" -type f -name apksigner | sort | tail -n 1
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --package-name)
      [[ $# -ge 2 ]] || die "Missing value for $1"
      PACKAGE_NAME="$2"
      shift 2
      ;;
    --version-name)
      [[ $# -ge 2 ]] || die "Missing value for $1"
      VERSION_NAME="$2"
      shift 2
      ;;
    --phone-version-code)
      [[ $# -ge 2 ]] || die "Missing value for $1"
      PHONE_VERSION_CODE="$2"
      shift 2
      ;;
    --wear-version-code)
      [[ $# -ge 2 ]] || die "Missing value for $1"
      WEAR_VERSION_CODE="$2"
      shift 2
      ;;
    --track)
      [[ $# -ge 2 ]] || die "Missing value for $1"
      PLAY_TRACK="$2"
      shift 2
      ;;
    --wear-track)
      [[ $# -ge 2 ]] || die "Missing value for $1"
      WEAR_PLAY_TRACK="$2"
      shift 2
      ;;
    --release-status)
      [[ $# -ge 2 ]] || die "Missing value for $1"
      RELEASE_STATUS="$2"
      shift 2
      ;;
    --output-directory)
      [[ $# -ge 2 ]] || die "Missing value for $1"
      OUTPUT_DIRECTORY="$2"
      shift 2
      ;;
    --build-only)
      BUILD_ONLY=true
      shift
      ;;
    --validate-only)
      VALIDATE_ONLY=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      die "Unknown argument: $1"
      ;;
  esac
done

[[ "$PACKAGE_NAME" =~ ^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z][A-Za-z0-9_]*)+$ ]] || \
  die "--package-name is not a valid Android application ID"

[[ -n "$VERSION_NAME" ]] || VERSION_NAME="$(generate_version_name)"
if [[ -z "$PHONE_VERSION_CODE" || -z "$WEAR_VERSION_CODE" ]]; then
  version_step="$(generate_version_step)"
  [[ "$version_step" =~ ^[1-9][0-9]*$ ]] || die "Generated version step is invalid"
  [[ -n "$PHONE_VERSION_CODE" ]] || PHONE_VERSION_CODE="$((PHONE_BUILD_FLOOR + version_step))"
  [[ -n "$WEAR_VERSION_CODE" ]] || WEAR_VERSION_CODE="$((WEAR_BUILD_FLOOR + version_step))"
fi

[[ "$PHONE_VERSION_CODE" =~ ^[1-9][0-9]*$ ]] || die "--phone-version-code must be a positive integer"
[[ "$WEAR_VERSION_CODE" =~ ^[1-9][0-9]*$ ]] || die "--wear-version-code must be a positive integer"
[[ "$PHONE_VERSION_CODE" -le 2100000000 ]] || die "--phone-version-code exceeds Google Play's limit"
[[ "$WEAR_VERSION_CODE" -le 2100000000 ]] || die "--wear-version-code exceeds Google Play's limit"

case "$PLAY_TRACK" in
  ''|*[!A-Za-z0-9._-]*) die "--track contains unsupported characters" ;;
esac

if [[ -z "$WEAR_PLAY_TRACK" ]]; then
  case "$PLAY_TRACK" in
    internal) WEAR_PLAY_TRACK="wear:qa" ;;
    beta) WEAR_PLAY_TRACK="wear:beta" ;;
    production) WEAR_PLAY_TRACK="wear:production" ;;
    *) WEAR_PLAY_TRACK="wear:$PLAY_TRACK" ;;
  esac
fi

case "$WEAR_PLAY_TRACK" in
  wear:) die "--wear-track must include a track name after wear:" ;;
  wear:*[!A-Za-z0-9._-]*) die "--wear-track contains unsupported characters" ;;
  wear:*) ;;
  *) die "--wear-track must start with wear:" ;;
esac

case "$RELEASE_STATUS" in
  draft|completed|halted|inProgress) ;;
  *) die "--release-status must be draft, completed, halted, or inProgress" ;;
esac

require_command ruby
require_command keytool
require_command jarsigner
[[ -x "$SCRIPT_DIRECTORY/gradlew" ]] || die "Gradle wrapper is missing or not executable"

if [[ "$BUILD_ONLY" == false ]]; then
  require_command bundle
  [[ -f "$SCRIPT_DIRECTORY/Gemfile" ]] || die "Gemfile not found at repository root"
fi

require_environment_variable SENSORBOX_ANDROID_KEY_ALIAS
require_environment_variable SENSORBOX_ANDROID_KEYSTORE_BASE64
require_environment_variable SENSORBOX_ANDROID_KEY_PASSWORD
require_environment_variable SENSORBOX_ANDROID_KEYSTORE_PASSWORD
if [[ "$BUILD_ONLY" == false ]]; then
  require_environment_variable GOOGLE_PLAY_SERVICE_ACCOUNT_BASE64
fi

APKSIGNER="$(find_apksigner)" || die "Android SDK apksigner was not found"

if [[ "$OUTPUT_DIRECTORY" != /* ]]; then
  OUTPUT_DIRECTORY="$SCRIPT_DIRECTORY/$OUTPUT_DIRECTORY"
fi

umask 077
release_tmp="$(mktemp -d "${TMPDIR:-/tmp}/sensorbox-android-release.XXXXXX")"
keystore_path="$release_tmp/upload.jks"
play_secret_path="$release_tmp/google-play-service-account.json"
export SENSORBOX_ANDROID_KEYSTORE_PATH="$keystore_path"

cleanup() {
  local exit_code=$?

  trap - EXIT HUP INT TERM
  unset \
    SENSORBOX_ANDROID_KEY_ALIAS \
    SENSORBOX_ANDROID_KEYSTORE_BASE64 \
    SENSORBOX_ANDROID_KEYSTORE_PATH \
    SENSORBOX_ANDROID_KEY_PASSWORD \
    SENSORBOX_ANDROID_KEYSTORE_PASSWORD \
    GOOGLE_PLAY_SERVICE_ACCOUNT_BASE64

  if [[ -n "${release_tmp:-}" && -d "$release_tmp" ]]; then
    if ! rm -rf -- "$release_tmp"; then
      printf 'Error: could not remove temporary credential directory: %s\n' "$release_tmp" >&2
      [[ $exit_code -ne 0 ]] || exit_code=1
    fi
  fi

  exit "$exit_code"
}

trap cleanup EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

ruby -rbase64 -e '
  encoded = ENV.fetch(ARGV.fetch(0)).gsub(/\s+/, "")
  File.binwrite(ARGV.fetch(1), Base64.strict_decode64(encoded))
' SENSORBOX_ANDROID_KEYSTORE_BASE64 "$keystore_path" || \
  die "Could not decode SENSORBOX_ANDROID_KEYSTORE_BASE64"

if [[ "$BUILD_ONLY" == false ]]; then
  ruby -rbase64 -rjson -e '
    encoded = ENV.fetch(ARGV.fetch(0)).gsub(/\s+/, "")
    decoded = Base64.strict_decode64(encoded)
    JSON.parse(decoded)
    File.binwrite(ARGV.fetch(1), decoded)
  ' GOOGLE_PLAY_SERVICE_ACCOUNT_BASE64 "$play_secret_path" || \
    die "GOOGLE_PLAY_SERVICE_ACCOUNT_BASE64 is not valid base64-encoded JSON"
fi

chmod 600 "$keystore_path"
if [[ "$BUILD_ONLY" == false ]]; then
  chmod 600 "$play_secret_path"
fi

keytool -list \
  -keystore "$keystore_path" \
  -storetype JKS \
  -storepass:env SENSORBOX_ANDROID_KEYSTORE_PASSWORD \
  -alias "$SENSORBOX_ANDROID_KEY_ALIAS" >/dev/null || \
  die "Could not open the JKS or find alias '$SENSORBOX_ANDROID_KEY_ALIAS'"

cd "$SCRIPT_DIRECTORY"

printf 'Using version %s, phone code %s, and Wear OS code %s.\n' \
  "$VERSION_NAME" "$PHONE_VERSION_CODE" "$WEAR_VERSION_CODE"

printf 'Building phone release bundle and APK...\n'
./gradlew :app:bundleRelease :app:assembleRelease --console=plain \
  -Psensorbox.versionName="$VERSION_NAME" \
  -Psensorbox.phoneVersionCode="$PHONE_VERSION_CODE"

printf 'Building Wear OS release bundle and APK...\n'
./gradlew :wear:bundleRelease :wear:assembleRelease --console=plain \
  -Psensorbox.versionName="$VERSION_NAME" \
  -Psensorbox.wearVersionCode="$WEAR_VERSION_CODE"

phone_aab="$SCRIPT_DIRECTORY/app/build/outputs/bundle/release/app-release.aab"
wear_aab="$SCRIPT_DIRECTORY/wear/build/outputs/bundle/release/wear-release.aab"
phone_apk="$SCRIPT_DIRECTORY/app/build/outputs/apk/release/app-release.apk"
wear_apk="$SCRIPT_DIRECTORY/wear/build/outputs/apk/release/wear-release.apk"
[[ -f "$phone_aab" ]] || die "Phone AAB not found: $phone_aab"
[[ -f "$wear_aab" ]] || die "Wear OS AAB not found: $wear_aab"
[[ -f "$phone_apk" ]] || die "Phone APK not found: $phone_apk"
[[ -f "$wear_apk" ]] || die "Wear OS APK not found: $wear_apk"

mkdir -p -- "$OUTPUT_DIRECTORY"
signed_phone_aab="$OUTPUT_DIRECTORY/sensorbox-phone-$PHONE_VERSION_CODE.aab"
signed_wear_aab="$OUTPUT_DIRECTORY/sensorbox-wear-$WEAR_VERSION_CODE.aab"
signed_phone_apk="$OUTPUT_DIRECTORY/sensorbox-phone-$PHONE_VERSION_CODE.apk"
signed_wear_apk="$OUTPUT_DIRECTORY/sensorbox-wear-$WEAR_VERSION_CODE.apk"

cp -- "$phone_aab" "$signed_phone_aab"
cp -- "$wear_aab" "$signed_wear_aab"
cp -- "$phone_apk" "$signed_phone_apk"
cp -- "$wear_apk" "$signed_wear_apk"

printf 'Verifying signed release artifacts...\n'
jarsigner -verify "$signed_phone_aab" >/dev/null || die "Phone AAB signature verification failed"
jarsigner -verify "$signed_wear_aab" >/dev/null || die "Wear OS AAB signature verification failed"
"$APKSIGNER" verify "$signed_phone_apk" || die "Phone APK signature verification failed"
"$APKSIGNER" verify "$signed_wear_apk" || die "Wear OS APK signature verification failed"

if [[ "$BUILD_ONLY" == true ]]; then
  printf 'Signed release build completed successfully.\nPhone AAB: %s\nPhone APK: %s\nWear OS AAB: %s\nWear OS APK: %s\n' \
    "$signed_phone_aab" "$signed_phone_apk" "$signed_wear_aab" "$signed_wear_apk"
  exit 0
fi

upload_bundle() {
  local aab=$1
  local track=$2
  local label=$3
  local fastlane_args=(
    android upload_bundle
    "aab:$aab"
    "json_key:$play_secret_path"
    "package_name:$PACKAGE_NAME"
    "track:$track"
    "release_status:$RELEASE_STATUS"
  )

  if [[ "$VALIDATE_ONLY" == true ]]; then
    fastlane_args+=("validate_only:true")
  fi

  printf 'Uploading %s bundle to the %s track...\n' "$label" "$track"
  bundle exec fastlane "${fastlane_args[@]}"
}

upload_bundle "$signed_phone_aab" "$PLAY_TRACK" "phone"
upload_bundle "$signed_wear_aab" "$WEAR_PLAY_TRACK" "Wear OS"

printf 'Release completed successfully.\nPhone AAB: %s\nPhone APK: %s\nWear OS AAB: %s\nWear OS APK: %s\n' \
  "$signed_phone_aab" "$signed_phone_apk" "$signed_wear_aab" "$signed_wear_apk"
