#!/bin/bash

set -u

# Check if argument is provided
if [ -z "${1:-}" ]; then
    echo "Error: Missing argument <deployedEnv>"
    echo "Usage: ./copy_build_packages.sh <deployedEnv>"
    exit 1
fi

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)

DEPLOYED_ENV=$1
UI_SOURCE="$REPO_ROOT/bad-court-mana-ui/build"
BACKEND_SEARCH_DIR="$REPO_ROOT/BadmintonCourtManagement/target"
OUTPUT_DIR="$SCRIPT_DIR"
UI_TEMP_DIR="$OUTPUT_DIR/$DEPLOYED_ENV"
UI_ZIP_FILE="$OUTPUT_DIR/$DEPLOYED_ENV.zip"

echo "--- Starting Package Collection for: $DEPLOYED_ENV ---"

# --- 1st Copy: React UI ---
if [ -d "$UI_SOURCE" ]; then
    # Rebuild the package from scratch so the zip always matches the latest UI build.
    rm -rf "$UI_TEMP_DIR" "$UI_ZIP_FILE"
    mkdir -p "$UI_TEMP_DIR"

    cp -R "$UI_SOURCE/." "$UI_TEMP_DIR/"
    (
        cd "$OUTPUT_DIR" || exit 1
        zip -rq "$UI_ZIP_FILE" "$DEPLOYED_ENV"
    )
    rm -rf "$UI_TEMP_DIR"
    echo "Success: Frontend packages zipped to $UI_ZIP_FILE"
else
    echo "Error: Source directory $UI_SOURCE does not exist. Skipping UI copy."
fi

# --- 2nd Copy: Spring Boot WAR ---
WAR_FILE=$(find "$BACKEND_SEARCH_DIR" -maxdepth 1 -type f -name "*.war" -print -quit)

if [ -f "$WAR_FILE" ]; then
    WAR_DEST="$OUTPUT_DIR/$(basename "$WAR_FILE")"
    
    # Replace only the WAR with the same filename and keep other WAR packages untouched.
    if [ -f "$WAR_DEST" ]; then
        rm -f "$WAR_DEST"
    fi

    cp -f "$WAR_FILE" "$WAR_DEST"
    echo "Success: Backend war file copied to $WAR_DEST"
else
    echo "Error: No .war file found in $BACKEND_SEARCH_DIR."
fi

echo "--- Script Finished ---"
exit 0
