#!/bin/sh

set -eu

if [ "$#" -ne 1 ]; then
    echo "Usage: $0 path/to/google_play" >&2
    exit 1
fi

PACK_DIR=$(CDPATH= cd -- "$1" && pwd)
ASSET_DIR="$PACK_DIR/assets"

command -v magick >/dev/null 2>&1 || {
    echo "ImageMagick 'magick' is required." >&2
    exit 1
}

validate_image() {
    file=$1
    expected_dimensions=$2
    maximum_bytes=$3

    if [ ! -f "$file" ]; then
        echo "Missing asset: $file" >&2
        exit 1
    fi

    dimensions=$(magick identify -format '%wx%h' "$file")
    if [ "$dimensions" != "$expected_dimensions" ]; then
        echo "Invalid dimensions for $file: $dimensions, expected $expected_dimensions" >&2
        exit 1
    fi

    bytes=$(stat -f '%z' "$file")
    if [ "$bytes" -gt "$maximum_bytes" ]; then
        echo "Asset is too large: $file is $bytes bytes" >&2
        exit 1
    fi
}

validate_no_alpha() {
    file=$1
    opaque=$(magick identify -format '%[opaque]' "$file")
    if [ "$opaque" != "True" ]; then
        echo "Asset must be fully opaque: $file" >&2
        exit 1
    fi
}

validate_icon_alpha() {
    file=$1
    channels=$(magick identify -format '%[channels]' "$file")
    case "$channels" in
        *rgba*|*graya*) ;;
        *)
            echo "App icon must contain an alpha channel: $file has channels $channels" >&2
            exit 1
            ;;
    esac
}

validate_image "$ASSET_DIR/app-icon-512.png" 512x512 1048576
validate_icon_alpha "$ASSET_DIR/app-icon-512.png"
validate_image "$ASSET_DIR/feature-graphic-1024x500.png" 1024x500 15728640
validate_no_alpha "$ASSET_DIR/feature-graphic-1024x500.png"

phone_count=0
for file in "$ASSET_DIR/phone"/*.png; do
    validate_image "$file" 1080x1920 8388608
    validate_no_alpha "$file"
    phone_count=$((phone_count + 1))
done

if [ "$phone_count" -lt 4 ]; then
    echo "At least four phone screenshots are required by this pack." >&2
    exit 1
fi

wear_count=0
for file in "$ASSET_DIR/wear"/*.png; do
    validate_image "$file" 454x454 8388608
    validate_no_alpha "$file"
    wear_count=$((wear_count + 1))
done

if [ "$wear_count" -lt 1 ]; then
    echo "At least one Wear OS screenshot is required." >&2
    exit 1
fi

echo "Validated $phone_count phone screenshots, $wear_count Wear OS screenshots, app icon, and feature graphic."
