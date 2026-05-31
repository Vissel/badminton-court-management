#!/bin/bash

set -u

# -----------------------------------------------------------
# copy_build_packages.sh — Build & collect deployment packages
#
# Usage: ./copy_build_packages.sh <deployedEnv>
#   deployedEnv: "qa"  →  caulong-tc-qa.zip  +  bad-court-management-qa.war
#                "prod" →  caulong-tc.zip     +  bad-court-management.war
# -----------------------------------------------------------

# ---- Argument validation ----
if [ $# -ne 1 ]; then
    echo "Error: Missing argument <deployedEnv>"
    echo "Usage: ./copy_build_packages.sh <deployedEnv>"
    echo "  deployedEnv: 'qa' or 'prod'"
    exit 1
fi

DEPLOYED_ENV=$1

if [ "$DEPLOYED_ENV" != "qa" ] && [ "$DEPLOYED_ENV" != "prod" ]; then
    echo "Error: Invalid argument '$DEPLOYED_ENV'. Must be 'qa' or 'prod'."
    echo "Usage: ./copy_build_packages.sh <deployedEnv>"
    exit 1
fi

# ---- Paths ----
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)
UI_DIR="$REPO_ROOT/bad-court-mana-ui"
BACKEND_DIR="$REPO_ROOT/BadmintonCourtManagement"
OUTPUT_DIR="$SCRIPT_DIR"

# ---- Profile-dependent naming ----
if [ "$DEPLOYED_ENV" = "qa" ]; then
    FRONTEND_ZIP_NAME="caulong-tc-qa"
    HOMEPAGE_PATH="/caulong-tc-qa/"
    MVN_PROFILE="qa"
    NPM_BUILD_CMD="build:qa"
else
    FRONTEND_ZIP_NAME="caulong-tc"
    HOMEPAGE_PATH="/caulong-tc/"
    MVN_PROFILE="prod"
    NPM_BUILD_CMD="build"
fi

echo "========================================"
echo "  Building & packaging for: $DEPLOYED_ENV"
echo "========================================"

# ===========================================================
# Step 1 — Build Backend (Spring Boot WAR)
# ===========================================================
echo ""
echo "[1/3] Building backend with Maven (profile: $MVN_PROFILE)..."
cd "$BACKEND_DIR" || {
    echo "Error: Backend directory not found: $BACKEND_DIR"
    exit 1
}

mvn clean package -P"$MVN_PROFILE" -DskipTests
MAVEN_EXIT=$?
if [ $MAVEN_EXIT -ne 0 ]; then
    echo "Error: Maven build failed (exit code: $MAVEN_EXIT)."
    exit 1
fi
echo "Success: Backend WAR built."

# ===========================================================
# Step 2 — Build Frontend (React UI)
# ===========================================================
echo ""
echo "[2/3] Building frontend with npm ($NPM_BUILD_CMD)..."
cd "$UI_DIR" || {
    echo "Error: Frontend directory not found: $UI_DIR"
    exit 1
}

# Temporarily set homepage in package.json for the target environment
PACKAGE_JSON="$UI_DIR/package.json"
HOMEPAGE_ORIGINAL=$(grep -o '"homepage": *"[^"]*"' "$PACKAGE_JSON" | head -1 | sed 's/"homepage": *"\(.*\)"/\1/')
echo "  -> Swapping homepage: '$HOMEPAGE_ORIGINAL' -> '$HOMEPAGE_PATH'"
sed -i '' 's|"homepage": *"[^"]*"|"homepage": "'"$HOMEPAGE_PATH"'"|' "$PACKAGE_JSON"

echo "  -> Installing dependencies..."
npm install --legacy-peer-deps
NPM_INSTALL_EXIT=$?
if [ $NPM_INSTALL_EXIT -ne 0 ]; then
    # Restore original homepage before exiting
    sed -i '' 's|"homepage": *"[^"]*"|"homepage": "'"$HOMEPAGE_ORIGINAL"'"|' "$PACKAGE_JSON"
    echo "Error: npm install failed (exit code: $NPM_INSTALL_EXIT)."
    exit 1
fi

echo "  -> Running npm run $NPM_BUILD_CMD..."
npm run "$NPM_BUILD_CMD"
NPM_BUILD_EXIT=$?

# Restore original homepage before checking build result
sed -i '' 's|"homepage": *"[^"]*"|"homepage": "'"$HOMEPAGE_ORIGINAL"'"|' "$PACKAGE_JSON"
echo "  -> Restored homepage: '$HOMEPAGE_ORIGINAL'"

if [ $NPM_BUILD_EXIT -ne 0 ]; then
    echo "Error: Frontend build failed (exit code: $NPM_BUILD_EXIT)."
    exit 1
fi
echo "Success: Frontend built."

# ===========================================================
# Step 3 — Collect packages into deployment/
# ===========================================================
echo ""
echo "[3/3] Collecting packages..."

# --- 3a. Frontend ZIP ---
UI_SOURCE="$UI_DIR/build"
UI_ZIP_FILE="$OUTPUT_DIR/$FRONTEND_ZIP_NAME.zip"
UI_TEMP_DIR="$OUTPUT_DIR/$FRONTEND_ZIP_NAME"

if [ ! -d "$UI_SOURCE" ]; then
    echo "Error: Frontend build output not found: $UI_SOURCE"
    exit 1
fi

rm -rf "$UI_TEMP_DIR" "$UI_ZIP_FILE"
mkdir -p "$UI_TEMP_DIR"

# Copy build output into a folder named caulong-tc / caulong-tc-qa
cp -R "$UI_SOURCE/." "$UI_TEMP_DIR/"

(
    cd "$OUTPUT_DIR" || exit 1
    zip -rq "$UI_ZIP_FILE" "$FRONTEND_ZIP_NAME"
)
rm -rf "$UI_TEMP_DIR"
echo "Success: Frontend ZIP created: $UI_ZIP_FILE"

# --- 3b. Backend WAR ---
WAR_FILE=$(find "$BACKEND_DIR/target" -maxdepth 1 -type f -name "*.war" -print -quit)

if [ -f "$WAR_FILE" ]; then
    WAR_DEST="$OUTPUT_DIR/$(basename "$WAR_FILE")"

    # Replace existing WAR with same name (keep other WARs untouched)
    if [ -f "$WAR_DEST" ]; then
        rm -f "$WAR_DEST"
    fi

    cp -f "$WAR_FILE" "$WAR_DEST"
    echo "Success: Backend WAR copied: $WAR_DEST"
else
    echo "Error: No .war file found in $BACKEND_DIR/target."
    exit 1
fi

echo ""
echo "========================================"
echo "  Package collection completed for: $DEPLOYED_ENV"
echo "  - ZIP: $UI_ZIP_FILE"
echo "  - WAR: $WAR_DEST"
echo "========================================"
exit 0
