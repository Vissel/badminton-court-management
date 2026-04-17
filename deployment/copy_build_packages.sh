#!/bin/bash

# Check if argument is provided
if [ -z "$1" ]; then
    echo "Error: Missing argument <deployedEnv>"
    echo "Usage: ./copy_build_packages.sh <deployedEnv>"
    exit 1
fi

DEPLOYED_ENV=$1
UI_SOURCE="../bad-court-mana-ui/build"
BACKEND_SEARCH_DIR="../BadmintonCourtManagement/target"

echo "--- Starting Package Collection for: $DEPLOYED_ENV ---"

# --- 1st Copy: React UI ---
if [ -d "$UI_SOURCE" ]; then
    # Remove existing env folder if it exists, then create fresh
    if [ -d "$DEPLOYED_ENV" ]; then
        rm -rf "$DEPLOYED_ENV"
    fi
    mkdir -p "$DEPLOYED_ENV"
    
    cp -R "$UI_SOURCE/" "$DEPLOYED_ENV/"
    echo "Success: Frontend packages copied to ./$DEPLOYED_ENV"
else
    echo "Error: Source directory $UI_SOURCE does not exist. Skipping UI copy."
fi

# --- 2nd Copy: Spring Boot WAR ---
# Find the first .war file in the target directory
WAR_FILE=$(find "$BACKEND_SEARCH_DIR" -maxdepth 1 -name "*.war" | head -n 1)

if [ -f "$WAR_FILE" ]; then
    cp "$WAR_FILE" .
    echo "Success: Backend war file $(basename "$WAR_FILE") copied to current directory."
else
    echo "Error: No .war file found in $BACKEND_SEARCH_DIR."
fi

echo "--- Script Finished ---"
exit 0