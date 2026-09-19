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
Build, sign, verify, and upload SensorBox phone and Wear OS Android App Bundles.

Usage:
  $SCRIPT_NAME \\
    [--package-name com.example.app] \\
    [--version-name 2026.09.19.12] \\
    [--phone-version-code 88] \\
    [--wear-version-code 1000050] \\
    [--track internal] \\
    [--wear-track wear:qa] \\
    [--release-status draft] \\
    [--output-directory PATH] \\
    [--validate-only]

Required environment variables:
  ANDROID_GRADLE_ALIAS
  ANDROID_GRADLE_BASE64_JKS
  ANDROID_GRADLE_KEY_PASSWORD
  ANDROID_GRADLE_KEYSTORE_PASSWORD
  ANDROID_SUPPLY_BASE64_SECRET

ANDROID_SUPPLY_BASE64_SECRET must contain the base64-encoded Google Play
service-account JSON key. When version values are omitted, the version name is
generated in UTC as yyyy.mm.dd.hh. Both version codes advance together from
their current phone and Wear OS floors using minute steps since BUILD_EPOCH.

The default destinations are draft releases on the phone internal track and
the Wear OS wear:qa track. Fastlane uploads only the signed bundles. Store
listing text, graphics, screenshots, and changelogs are skipped.
Use --validate-only to ask Google Play to validate without committing a release.
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

if [[ -z "$VERSION_NAME" ]]; then
  VERSION_NAME="$(generate_version_name)"
fi

if [[ -z "$PHONE_VERSION_CODE" || -z "$WEAR_VERSION_CODE" ]]; then
  version_step="$(generate_version_step)"
  [[ "$version_step" =~ ^[1-9][0-9]*$ ]] || die "Generated version step is invalid"
  [[ -n "$PHONE_VERSION_CODE" ]] || PHONE_VERSION_CODE="$((PHONE_BUILD_FLOOR + version_step))"
  [[ -n "$WEAR_VERSION_CODE" ]] || WEAR_VERSION_CODE="$((WEAR_BUILD_FLOOR + version_step))"
fi

[[ -n "$VERSION_NAME" ]] || die "Could not determine --version-name"
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
require_command zip
require_command unzip
require_command bundle

[[ -x "$SCRIPT_DIRECTORY/gradlew" ]] || die "Gradle wrapper is missing or not executable"
[[ -f "$SCRIPT_DIRECTORY/Gemfile" ]] || die "Gemfile not found at repository root"

require_environment_variable ANDROID_GRADLE_ALIAS
require_environment_variable ANDROID_GRADLE_BASE64_JKS
require_environment_variable ANDROID_GRADLE_KEY_PASSWORD
require_environment_variable ANDROID_GRADLE_KEYSTORE_PASSWORD
require_environment_variable ANDROID_SUPPLY_BASE64_SECRET

if [[ "$OUTPUT_DIRECTORY" != /* ]]; then
  OUTPUT_DIRECTORY="$SCRIPT_DIRECTORY/$OUTPUT_DIRECTORY"
fi

umask 077
release_tmp="$(mktemp -d "${TMPDIR:-/tmp}/sensorbox-android-release.XXXXXX")"
keystore_path="$release_tmp/upload.jks"
play_secret_path="$release_tmp/google-play-service-account.json"

cleanup() {
  local exit_code=$?

  trap - EXIT HUP INT TERM
  unset \
    ANDROID_GRADLE_ALIAS \
    ANDROID_GRADLE_BASE64_JKS \
    ANDROID_GRADLE_KEY_PASSWORD \
    ANDROID_GRADLE_KEYSTORE_PASSWORD \
    ANDROID_SUPPLY_BASE64_SECRET

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
' ANDROID_GRADLE_BASE64_JKS "$keystore_path" || die "Could not decode ANDROID_GRADLE_BASE64_JKS"

ruby -rbase64 -rjson -e '
  encoded = ENV.fetch(ARGV.fetch(0)).gsub(/\s+/, "")
  decoded = Base64.strict_decode64(encoded)
  JSON.parse(decoded)
  File.binwrite(ARGV.fetch(1), decoded)
' ANDROID_SUPPLY_BASE64_SECRET "$play_secret_path" || \
  die "ANDROID_SUPPLY_BASE64_SECRET is not valid base64-encoded JSON"

chmod 600 "$keystore_path" "$play_secret_path"

keytool -list \
  -keystore "$keystore_path" \
  -storetype JKS \
  -storepass:env ANDROID_GRADLE_KEYSTORE_PASSWORD \
  -alias "$ANDROID_GRADLE_ALIAS" >/dev/null || \
  die "Could not open the JKS or find alias '$ANDROID_GRADLE_ALIAS'"

cd "$SCRIPT_DIRECTORY"

printf 'Using version %s, phone code %s, and Wear OS code %s.\n' \
  "$VERSION_NAME" "$PHONE_VERSION_CODE" "$WEAR_VERSION_CODE"

printf 'Building phone release bundle...\n'
./gradlew :app:bundleRelease --console=plain \
  -Psensorbox.versionName="$VERSION_NAME" \
  -Psensorbox.phoneVersionCode="$PHONE_VERSION_CODE"

printf 'Building Wear OS release bundle...\n'
./gradlew :wear:bundleRelease --console=plain \
  -Psensorbox.versionName="$VERSION_NAME" \
  -Psensorbox.wearVersionCode="$WEAR_VERSION_CODE"

phone_aab="$SCRIPT_DIRECTORY/app/build/outputs/bundle/release/app-release.aab"
wear_aab="$SCRIPT_DIRECTORY/wear/build/outputs/bundle/release/wear-release.aab"
[[ -f "$phone_aab" ]] || die "Phone AAB not found: $phone_aab"
[[ -f "$wear_aab" ]] || die "Wear OS AAB not found: $wear_aab"

mkdir -p -- "$OUTPUT_DIRECTORY"
signed_phone_aab="$OUTPUT_DIRECTORY/sensorbox-phone-$PHONE_VERSION_CODE.aab"
signed_wear_aab="$OUTPUT_DIRECTORY/sensorbox-wear-$WEAR_VERSION_CODE.aab"

sign_bundle() {
  local source_aab=$1
  local signed_aab=$2
  local unsigned_copy=$3
  local label=$4
  local signature_entries=()

  cp -- "$source_aab" "$unsigned_copy"
  while IFS= read -r entry; do
    signature_entries+=("$entry")
  done < <(unzip -Z1 "$unsigned_copy" | grep -E '^META-INF/[^/]+\.(SF|RSA|DSA|EC)$' || true)

  if [[ ${#signature_entries[@]} -gt 0 ]]; then
    zip -q -d "$unsigned_copy" "${signature_entries[@]}"
  fi

  printf 'Signing %s bundle with alias %s...\n' "$label" "$ANDROID_GRADLE_ALIAS"
  jarsigner \
    -keystore "$keystore_path" \
    -storetype JKS \
    -storepass:env ANDROID_GRADLE_KEYSTORE_PASSWORD \
    -keypass:env ANDROID_GRADLE_KEY_PASSWORD \
    -signedjar "$signed_aab" \
    "$unsigned_copy" \
    "$ANDROID_GRADLE_ALIAS"

  printf 'Verifying %s bundle signature...\n' "$label"
  jarsigner -verify "$signed_aab" >/dev/null || die "$label AAB signature verification failed"
}

sign_bundle "$phone_aab" "$signed_phone_aab" "$release_tmp/phone-unsigned.aab" "phone"
sign_bundle "$wear_aab" "$signed_wear_aab" "$release_tmp/wear-unsigned.aab" "Wear OS"

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

printf 'Release completed successfully.\nPhone AAB: %s\nWear OS AAB: %s\n' \
  "$signed_phone_aab" "$signed_wear_aab"
