#!/bin/bash

# JDK 21 Setup Verification Script
# This script checks if JDK 21 is properly configured for this project

echo "========================================="
echo "JDK 21 Setup Verification"
echo "========================================="
echo ""

# Check if Java is installed
if command -v java &> /dev/null; then
    echo "✓ Java is installed"
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    echo "  Version: $JAVA_VERSION"
    
    # Check if it's JDK 21
    if java -version 2>&1 | grep -q "version \"21"; then
        echo "✓ JDK 21 is correctly installed"
    else
        echo "✗ WARNING: JDK 21 is not the active version"
        echo "  Please install and activate JDK 21"
    fi
else
    echo "✗ Java is not installed"
    echo "  Please install JDK 21 first"
fi

echo ""

# Check if javac is available
if command -v javac &> /dev/null; then
    echo "✓ javac (Java Compiler) is installed"
    JAVAC_VERSION=$(javac -version 2>&1)
    echo "  Version: $JAVAC_VERSION"
else
    echo "✗ javac (Java Compiler) is not installed"
    echo "  Please install JDK (not just JRE)"
fi

echo ""

# Check JAVA_HOME
if [ -n "$JAVA_HOME" ]; then
    echo "✓ JAVA_HOME is set: $JAVA_HOME"
else
    echo "⚠ WARNING: JAVA_HOME is not set"
    echo "  Consider setting JAVA_HOME in your shell profile"
fi

echo ""

# Check Maven
if command -v mvn &> /dev/null; then
    echo "✓ Maven is installed"
    MVN_VERSION=$(mvn -version 2>&1 | head -n 1)
    echo "  Version: $MVN_VERSION"
    
    # Check Maven's Java version
    MVN_JAVA=$(mvn -version 2>&1 | grep "Java version")
    echo "  $MVN_JAVA"
else
    echo "✗ Maven is not installed"
    echo "  Please install Maven 3.8+"
fi

echo ""

# Check Maven wrapper
if [ -f "./mvnw" ]; then
    echo "✓ Maven wrapper (mvnw) is available"
else
    echo "⚠ WARNING: Maven wrapper not found"
fi

echo ""

# Check SDKMAN
if [ -d "$HOME/.sdkman" ]; then
    echo "✓ SDKMAN! is installed"
    
    # Check .sdkmanrc
    if [ -f ".sdkmanrc" ]; then
        echo "✓ .sdkmanrc file exists"
        echo "  Configured JDK: $(grep 'java=' .sdkmanrc)"
    else
        echo "⚠ WARNING: .sdkmanrc file not found"
    fi
else
    echo "ℹ SDKMAN! is not installed (optional)"
    echo "  Install with: curl -s \"https://get.sdkman.io\" | bash"
fi

echo ""
echo "========================================="
echo "Installation Instructions (if needed):"
echo "========================================="
echo ""
echo "Option 1: Using SDKMAN! (Recommended)"
echo "  curl -s \"https://get.sdkman.io\" | bash"
echo "  sdk install java 21.0.1-tem"
echo "  sdk default java 21.0.1-tem"
echo ""
echo "Option 2: Using Homebrew (macOS)"
echo "  brew install temurin@21"
echo ""
echo "Option 3: Manual Download"
echo "  https://adoptium.net/"
echo ""
echo "========================================="
