# JDK 21 Setup Guide

This project requires **JDK 21** (Java 21 LTS) for development and deployment.

## Prerequisites

- JDK 21 (Temurin, Oracle, or OpenJDK)
- Maven 3.8+
- IDE: IntelliJ IDEA, Eclipse, or VS Code

## Installing JDK 21

### Option 1: Using SDKMAN! (Recommended for macOS/Linux)

```bash
# Install SDKMAN! if not already installed
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install JDK 21 (Temurin)
sdk install java 21.0.1-tem

# Set as default
sdk default java 21.0.1-tem
```

This project includes a `.sdkmanrc` file that will automatically switch to JDK 21 when you enter the project directory (if SDKMAN! autoenv is enabled).

### Option 2: Using Homebrew (macOS)

```bash
brew install temurin@21
```

### Option 3: Manual Installation

Download JDK 21 from:
- [Eclipse Temurin](https://adoptium.net/)
- [Oracle JDK](https://www.oracle.com/java/technologies/downloads/#java21)
- [OpenJDK](https://jdk.java.net/21/)

## Verifying JDK Installation

```bash
java -version
# Should output: openjdk version "21.x.x" or similar

javac -version
# Should output: javac 21.x.x
```

## Project Configuration

### Maven (pom.xml)

The project is configured to use JDK 21 in `pom.xml`:

```xml
<properties>
    <java.version>21</java.version>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <maven.compiler.release>21</maven.compiler.release>
</properties>
```

### IDE Configuration

#### IntelliJ IDEA
The `.idea/misc.xml` and `.idea/compiler.xml` files are already configured for JDK 21.

#### VS Code / Eclipse
The `.vscode/settings.json` file configures the Java runtime to use JDK 21.

## Building the Project

```bash
# Using Maven wrapper
./mvnw clean install

# Or using system Maven
mvn clean install

# Build with specific profile
./mvnw clean package -P dev    # Development
./mvnw clean package -P qa     # QA
./mvnw clean package -P prod   # Production
```

## Running the Project

```bash
# Development mode
./mvnw spring-boot:run -P dev

# Or run the built WAR file
java -jar target/bad-court-management-dev.war
```

## Troubleshooting

### Wrong JDK Version

If you're getting compilation errors related to Java version:

```bash
# Check which Java is being used
which java
java -version

# For SDKMAN! users, ensure autoenv is working
sdk env
```

### Maven Using Wrong JDK

```bash
# Check Maven's Java version
mvn -version

# Force Maven to use specific Java
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
mvn clean install
```

### IDE Issues

**IntelliJ IDEA:**
1. File → Project Structure → Project SDK → Select JDK 21
2. File → Project Structure → Modules → Language level → 21
3. Settings → Build, Execution, Deployment → Compiler → Java Compiler → Target bytecode version → 21

**VS Code:**
- The `.vscode/settings.json` file should automatically configure JDK 21
- If issues persist, run: `Java: Clean Java Language Server Workspace`

## Notes

- This project uses Spring Boot 3.5.3, which requires JDK 21 as a minimum
- The project is packaged as a WAR file for deployment to external Tomcat
- All Maven profiles (dev, qa, prod) are configured to use JDK 21
