# JDK 21 Environment Setup Script
# Source this file to set up JDK 21 for this project
# Usage: source setup-jdk21.sh

# Detect OS and set JDK path accordingly
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS - Try common JDK 21 locations
    if [ -d "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home" ]; then
        export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home"
    elif [ -d "/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home" ]; then
        export JAVA_HOME="/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home"
    else
        # Try to find JDK 21 using /usr/libexec
        export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null)
    fi
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux - Try common locations
    if [ -d "/usr/lib/jvm/temurin-21-jdk" ]; then
        export JAVA_HOME="/usr/lib/jvm/temurin-21-jdk"
    elif [ -d "/usr/lib/jvm/java-21-openjdk" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-21-openjdk"
    elif [ -d "/usr/lib/jvm/jdk-21" ]; then
        export JAVA_HOME="/usr/lib/jvm/jdk-21"
    fi
fi

# Add Java to PATH
if [ -n "$JAVA_HOME" ] && [ -d "$JAVA_HOME" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
    echo "✓ JDK 21 environment configured"
    echo "  JAVA_HOME: $JAVA_HOME"
    echo "  Java version: $(java -version 2>&1 | head -n 1)"
else
    echo "✗ Could not find JDK 21 installation"
    echo "  Please install JDK 21 first:"
    echo "  - macOS: brew install temurin@21"
    echo "  - Linux: sdk install java 21.0.1-tem"
    echo "  - Or download from: https://adoptium.net/"
fi
