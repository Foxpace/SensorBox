#!/bin/sh

set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPOSITORY_ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
OUTPUT_DIR="$SCRIPT_DIR/assets"
PHONE_OUTPUT_DIR="$OUTPUT_DIR/phone"
WEAR_OUTPUT_DIR="$OUTPUT_DIR/wear"
FASTLANE_DIR="$REPOSITORY_ROOT/fastlane/metadata/android/en-GB/images"
FEATURE_SOURCE="$SCRIPT_DIR/source/feature-graphic.png"

PHONE_SOURCE_DIR="$REPOSITORY_ROOT/app/src/screenshotTestDebug/reference/com/tomasrepcik/sensorbox/navigation/ReadmePhoneScreenshotPreviewsKt"
WEAR_SOURCE_DIR="$REPOSITORY_ROOT/wear/src/screenshotTestDebug/reference/com/tomasrepcik/sensorbox/home/ReadmeWearScreenshotPreviewsKt"

command -v magick >/dev/null 2>&1 || {
    echo "ImageMagick 'magick' is required." >&2
    exit 1
}

find_source() {
    directory=$1
    pattern=$2
    source=$(find "$directory" -maxdepth 1 -type f -name "$pattern" -print -quit)
    if [ -z "$source" ]; then
        echo "Missing screenshot source: $directory/$pattern" >&2
        exit 1
    fi
    printf '%s\n' "$source"
}

prepare_phone() {
    source=$1
    destination=$2
    magick "$source" \
        -resize 864x1920 \
        -gravity center \
        -background '#101010' \
        -extent 1080x1920 \
        -alpha off \
        -strip \
        "$destination"
}

prepare_wear() {
    source=$1
    destination=$2
    magick "$source" -alpha off -strip "$destination"
}

mkdir -p "$PHONE_OUTPUT_DIR" "$WEAR_OUTPUT_DIR"
mkdir -p "$FASTLANE_DIR/phoneScreenshots" "$FASTLANE_DIR/wearScreenshots"

magick "$REPOSITORY_ROOT/AppImages/icon.png" -strip "$OUTPUT_DIR/app-icon-512.png"
magick "$FEATURE_SOURCE" -alpha off -strip "$OUTPUT_DIR/feature-graphic-1024x500.png"

prepare_phone \
    "$(find_source "$PHONE_SOURCE_DIR" 'readmeSourceSelection_*.png')" \
    "$PHONE_OUTPUT_DIR/01-choose-sources.png"
prepare_phone \
    "$(find_source "$PHONE_SOURCE_DIR" 'readmeRecordingSetup_*.png')" \
    "$PHONE_OUTPUT_DIR/02-configure-recording.png"
prepare_phone \
    "$(find_source "$PHONE_SOURCE_DIR" 'readmeIntroPrivacy_*.png')" \
    "$PHONE_OUTPUT_DIR/03-local-private-recording.png"
prepare_phone \
    "$(find_source "$PHONE_SOURCE_DIR" 'readmeIntroStorage_*.png')" \
    "$PHONE_OUTPUT_DIR/04-choose-recording-archive.png"

prepare_wear \
    "$(find_source "$WEAR_SOURCE_DIR" 'readmeWearDashboard_*.png')" \
    "$WEAR_OUTPUT_DIR/01-dashboard.png"
prepare_wear \
    "$(find_source "$WEAR_SOURCE_DIR" 'wearRecordSelection_*.png')" \
    "$WEAR_OUTPUT_DIR/02-choose-sources.png"
prepare_wear \
    "$(find_source "$WEAR_SOURCE_DIR" 'WearActiveRecording_*.png')" \
    "$WEAR_OUTPUT_DIR/03-active-recording.png"
prepare_wear \
    "$(find_source "$WEAR_SOURCE_DIR" 'readmeWearLivePicker_*.png')" \
    "$WEAR_OUTPUT_DIR/04-live-sensor.png"

"$REPOSITORY_ROOT/scripts/validate_assets.sh" "$SCRIPT_DIR"

cp "$OUTPUT_DIR/app-icon-512.png" "$FASTLANE_DIR/icon.png"
cp "$OUTPUT_DIR/feature-graphic-1024x500.png" "$FASTLANE_DIR/featureGraphic.png"

phone_index=1
for source in "$PHONE_OUTPUT_DIR"/*.png; do
    cp "$source" "$FASTLANE_DIR/phoneScreenshots/${phone_index}_en-GB.png"
    phone_index=$((phone_index + 1))
done

wear_index=1
for source in "$WEAR_OUTPUT_DIR"/*.png; do
    cp "$source" "$FASTLANE_DIR/wearScreenshots/${wear_index}_en-GB.png"
    wear_index=$((wear_index + 1))
done

echo "Google Play assets prepared and mirrored to Fastlane metadata."
