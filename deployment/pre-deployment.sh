#!/bin/bash

# Check if argument is provided
if [ -z "$1" ]; then
    echo "Error: Missing argument <location>"
    echo "Usage: ./pre-deployment.sh <location>"
    exit 1
fi

DEST_LOCATION=$1

echo "--- Starting Pre-Deployment to: $DEST_LOCATION ---"

# Check for existence of at least one directory and one .war file
UI_FOLDER=$(find . -maxdepth 1 -type d ! -name "." ! -name ".*" ! -name "bad-court-ui" ! -name "BadmintonCourtManagement" | head -n 1)
WAR_FILE=$(find . -maxdepth 1 -name "*.war" | head -n 1)

# Validation logic
if [ -n "$UI_FOLDER" ] && [ -f "$WAR_FILE" ]; then
    echo "Packages verified: Found folder '$UI_FOLDER' and file '$WAR_FILE'."
    
    # Ensure destination exists
    mkdir -p "$DEST_LOCATION"
    
    # Copying to destination
    cp -R "$UI_FOLDER" "$DEST_LOCATION/"
    cp "$WAR_FILE" "$DEST_LOCATION/"
    
    if [ $? -eq 0 ]; then
        echo "Success: Deployment packages moved to $DEST_LOCATION successfully."
    else
        echo "Error: An error occurred during the file transfer."
        exit 1
    fi
else
    echo "Error: Deployment failed. Ensure one frontend folder and one .war file exist in the current directory."
    exit 1
fi

echo "--- Script Finished ---"
exit 0