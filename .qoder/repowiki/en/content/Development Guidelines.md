# Development Guidelines

<cite>
**Referenced Files in This Document**
- [README.md](file://README.md)
- [JDK21_SETUP.md](file://BadmintonCourtManagement/JDK21_SETUP.md)
- [setup-jdk21.sh](file://BadmintonCourtManagement/setup-jdk21.sh)
- [check-jdk21.sh](file://BadmintonCourtManagement/check-jdk21.sh)
- [pom.xml](file://BadmintonCourtManagement/pom.xml)
- [application.properties](file://BadmintonCourtManagement/src/main/resources/application.properties)
- [application-dev.properties](file://BadmintonCourtManagement/src/main/resources/application-dev.properties)
- [application-qa.properties](file://BadmintonCourtManagement/src/main/resources/application-qa.properties)
- [application-prod.properties](file://BadmintonCourtManagement/src/main/resources/application-prod.properties)
- [BadmintonCourtManagementApplication.java](file://BadmintonCourtManagement/src/main/java/com/badminton/BadmintonCourtManagementApplication.java)
- [SecurityConfig.java](file://BadmintonCourtManagement/src/main/java/com/badminton/config/SecurityConfig.java)
- [AuthenController.java](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/AuthenController.java)
- [package.json](file://bad-court-mana-ui/package.json)
- [App.js](file://bad-court-mana-ui/src/App.js)
- [index.js](file://bad-court-mana-ui/src/index.js)
- [AuthContext.js](file://bad-court-mana-ui/src/context/AuthContext.js)
- [ProtectedRoute.js](file://bad-court-mana-ui/src/context/ProtectedRoute.js)
- [copy_build_packages.sh](file://deployment/copy_build_packages.sh)
- [pre-deployment.sh](file://deployment/pre-deployment.sh)
</cite>

## Update Summary
**Changes Made**
- Enhanced Maven build configuration with Lombok annotation processor paths in all profiles
- Improved build system setup with consistent JDK 21 compiler configuration
- Updated frontend build scripts with new build:qa capability using env-cmd
- Added comprehensive build optimization documentation for development workflow
- Updated deployment automation with environment-specific build processes

## Table of Contents
1. [Introduction](#introduction)
2. [Project Structure](#project-structure)
3. [Core Components](#core-components)
4. [Architecture Overview](#architecture-overview)
5. [Detailed Component Analysis](#detailed-component-analysis)
6. [Dependency Analysis](#dependency-analysis)
7. [Performance Considerations](#performance-considerations)
8. [Troubleshooting Guide](#troubleshooting-guide)
9. [Conclusion](#conclusion)
10. [Appendices](#appendices)

## Introduction
This document provides comprehensive development guidelines for the Badminton Court Management system. It covers JDK 21 setup, development environment configuration, Maven project structure and build optimization, React frontend organization and best practices, code style and naming conventions, code review and branching workflows, and the end-to-end development lifecycle from feature implementation to testing and deployment. The system follows a monorepo-style layout with a Spring Boot backend and a React SPA frontend communicating over REST APIs with CORS configured for local development.

**Updated** Enhanced JDK 21 migration requirements and new development setup procedures are now fully documented, including automated verification scripts, Lombok annotation processor configuration, and advanced build optimization techniques.

## Project Structure
The repository is organized as a monorepo containing:
- Backend: Spring Boot application under BadmintonCourtManagement with JDK 21 requirements and enhanced Maven build configuration
- Frontend: React SPA under bad-court-mana-ui with React 19 and modern tooling including new build:qa capability
- Deployment scripts and SQL assets under deployment and sql
- Top-level documentation and notes

```mermaid
graph TB
subgraph "Backend (Spring Boot 3.5.x + JDK 21)"
A["BadmintonCourtManagement<br/>JDK 21 + Spring Boot 3.5.x<br/>Enhanced Maven Build"]
end
subgraph "Frontend (React SPA 19)"
B["bad-court-mana-ui<br/>React 19 + React Router v7 + Material-UI<br/>build:qa Support"]
end
subgraph "Operations"
C["deployment/<br/>Advanced Build Scripts"]
D["sql/<br/>Liquibase + DB changesets"]
E["copy_build_packages.sh<br/>Multi-env Build Automation"]
F["pre-deployment.sh<br/>Package Validation"]
end
G["README.md<br/>High-level overview"]
B --> |"REST API"| A
A --> |"DB migrations"| D
A --> |"Build artifacts"| C
G --> A
G --> B
```

**Diagram sources**
- [README.md:56-81](file://README.md#L56-L81)
- [pom.xml:1-225](file://BadmintonCourtManagement/pom.xml#L1-L225)
- [package.json:1-59](file://bad-court-mana-ui/package.json#L1-L59)
- [copy_build_packages.sh:1-165](file://deployment/copy_build_packages.sh#L1-L165)

**Section sources**
- [README.md:54-103](file://README.md#L54-L103)
- [pom.xml:1-225](file://BadmintonCourtManagement/pom.xml#L1-L225)
- [package.json:1-59](file://bad-court-mana-ui/package.json#L1-L59)

## Core Components
- Backend application entrypoint initializes Spring Boot and enables scheduling with JDK 21 compatibility.
- Security configuration defines CSRF and CORS policies, session management, and authentication provider.
- Authentication controller handles login and CSRF token validation.
- Frontend app bootstraps routing, theming, and protected routes with an authentication context provider.
- Enhanced Maven build configuration with Lombok annotation processor support across all profiles.

Key implementation references:
- Application bootstrap: [BadmintonCourtManagementApplication.java:1-16](file://BadmintonCourtManagement/src/main/java/com/badminton/BadmintonCourtManagementApplication.java#L1-L16)
- Security configuration: [SecurityConfig.java:1-193](file://BadmintonCourtManagement/src/main/java/com/badminton/config/SecurityConfig.java#L1-L193)
- Authentication controller: [AuthenController.java:1-111](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/AuthenController.java#L1-L111)
- Frontend app routing and providers: [App.js:1-104](file://bad-court-mana-ui/src/App.js#L1-L104)
- Frontend auth provider and session handling: [AuthContext.js:1-91](file://bad-court-mana-ui/src/context/AuthContext.js#L1-L91)
- Enhanced Maven configuration: [pom.xml:115-222](file://BadmintonCourtManagement/pom.xml#L115-L222)

**Section sources**
- [BadmintonCourtManagementApplication.java:1-16](file://BadmintonCourtManagement/src/main/java/com/badminton/BadmintonCourtManagementApplication.java#L1-L16)
- [SecurityConfig.java:1-193](file://BadmintonCourtManagement/src/main/java/com/badminton/config/SecurityConfig.java#L1-L193)
- [AuthenController.java:1-111](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/AuthenController.java#L1-L111)
- [App.js:1-104](file://bad-court-mana-ui/src/App.js#L1-L104)
- [AuthContext.js:1-91](file://bad-court-mana-ui/src/context/AuthContext.js#L1-L91)
- [pom.xml:115-222](file://BadmintonCourtManagement/pom.xml#L115-L222)

## Architecture Overview
The system uses a session-centric domain model with a clear separation between frontend and backend:
- Backend exposes REST endpoints for authentication, session management, court/game administration, payment, and reporting.
- Frontend is a React SPA that authenticates via CSRF-enabled endpoints and interacts with the backend through Axios.
- CORS is configured for local development (frontend on port 3000, backend on 8080) and secured for production.
- Enhanced build system supports multiple environments with optimized compilation and Lombok support.

```mermaid
graph TB
FE["React SPA 19<br/>bad-court-mana-ui<br/>build:qa Support"]
AUTH["Auth Controller<br/>/login, /logout, /csrf"]
SEC["SecurityConfig<br/>CSRF, CORS, Session"]
APP["Spring Boot App<br/>@SpringBootApplication + @EnableScheduling<br/>Lombok Annotation Processing"]
DB["MySQL + Liquibase"]
DEPLOY["Build Automation<br/>copy_build_packages.sh<br/>Environment-specific Builds"]
FE --> |"Axios + js-cookie"| AUTH
AUTH --> SEC
SEC --> APP
APP --> DB
DEPLOY --> APP
DEPLOY --> FE
```

**Diagram sources**
- [README.md:89-103](file://README.md#L89-L103)
- [SecurityConfig.java:44-92](file://BadmintonCourtManagement/src/main/java/com/badminton/config/SecurityConfig.java#L44-L92)
- [AuthenController.java:78-111](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/AuthenController.java#L78-L111)
- [application-dev.properties:1-12](file://BadmintonCourtManagement/src/main/resources/application-dev.properties#L1-L12)
- [copy_build_packages.sh:36-47](file://deployment/copy_build_packages.sh#L36-L47)

**Section sources**
- [README.md:89-103](file://README.md#L89-L103)
- [SecurityConfig.java:112-140](file://BadmintonCourtManagement/src/main/java/com/badminton/config/SecurityConfig.java#L112-L140)
- [AuthenController.java:45-73](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/AuthenController.java#L45-L73)

## Detailed Component Analysis

### Backend: Enhanced Maven Build Configuration
The Maven build configuration now includes comprehensive Lombok annotation processor support across all profiles (dev, qa, prod). Each profile maintains consistent JDK 21 compiler settings with proper annotation processing configuration.

```mermaid
sequenceDiagram
participant Dev as "Developer"
participant Maven as "Maven Build System"
participant Lombok as "Lombok Processor"
participant Compiler as "JDK 21 Compiler"
Dev->>Maven : mvn -P{profile} clean package
Maven->>Compiler : Configure JDK 21 settings
Maven->>Lombok : Load annotation processor paths
Lombok->>Compiler : Process @Data, @Getter, @Setter annotations
Compiler-->>Maven : Generate bytecode with processed annotations
Maven-->>Dev : Produce WAR artifact with Lombok support
```

**Diagram sources**
- [pom.xml:121-146](file://BadmintonCourtManagement/pom.xml#L121-L146)
- [pom.xml:156-181](file://BadmintonCourtManagement/pom.xml#L156-L181)
- [pom.xml:192-221](file://BadmintonCourtManagement/pom.xml#L192-L221)

**Section sources**
- [pom.xml:121-146](file://BadmintonCourtManagement/pom.xml#L121-L146)
- [pom.xml:156-181](file://BadmintonCourtManagement/pom.xml#L156-L181)
- [pom.xml:192-221](file://BadmintonCourtManagement/pom.xml#L192-L221)

### Frontend: Advanced Build System with QA Support
The React frontend now includes a dedicated build:qa script that utilizes env-cmd to load environment-specific configuration files (.env.qa) for QA environment deployments.

```mermaid
flowchart TD
Start(["npm run build:qa"]) --> EnvCheck["Check .env.qa exists"]
EnvCheck --> LoadEnv["Load environment variables via env-cmd"]
LoadEnv --> ProcessAssets["Process React application assets"]
ProcessAssets --> GenerateBuild["Generate production build"]
GenerateBuild --> Output["Output to build/ directory"]
```

**Diagram sources**
- [package.json:30-36](file://bad-court-mana-ui/package.json#L30-L36)
- [copy_build_packages.sh:97-99](file://deployment/copy_build_packages.sh#L97-L99)

**Section sources**
- [package.json:30-36](file://bad-court-mana-ui/package.json#L30-L36)
- [copy_build_packages.sh:97-99](file://deployment/copy_build_packages.sh#L97-L99)

### Backend: Authentication Flow
The authentication flow integrates Spring Security with CSRF protection and session management. The frontend obtains a CSRF token and stores it in cookies, then logs in to establish a session.

```mermaid
sequenceDiagram
participant Browser as "Browser"
participant FE as "React SPA 19"
participant API as "Auth Controller"
participant Sec as "SecurityConfig"
participant Sess as "HttpSession"
Browser->>FE : Load app
FE->>API : GET /csrf
API->>Sec : Validate session and token age
API-->>FE : {csrfToken, valid, expiresInSeconds}
FE->>API : POST /login {username,password}
API->>Sess : Create session, store CSRF creation time
API-->>FE : {message, username, csrfToken}
FE->>Browser : Store cookies (CSRF, session)
```

**Diagram sources**
- [AuthenController.java:45-111](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/AuthenController.java#L45-L111)
- [SecurityConfig.java:44-92](file://BadmintonCourtManagement/src/main/java/com/badminton/config/SecurityConfig.java#L44-L92)
- [AuthContext.js:14-32](file://bad-court-mana-ui/src/context/AuthContext.js#L14-L32)

**Section sources**
- [AuthenController.java:78-111](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/AuthenController.java#L78-L111)
- [AuthContext.js:14-32](file://bad-court-mana-ui/src/context/AuthContext.js#L14-L32)

### Frontend: Routing and Protected Routes
The React app sets up routing with protected routes and an authentication context provider. Theming is centralized via Material-UI.

```mermaid
flowchart TD
Start(["App Mount"]) --> InitAuth["AuthProvider init<br/>GET /csrf"]
InitAuth --> TokenOK{"CSRF valid?"}
TokenOK --> |Yes| SetCtx["Set authenticated + CSRF token"]
TokenOK --> |No| ClearCtx["Clear state and redirect to login"]
SetCtx --> Routes["Render Routes"]
Routes --> Protected["ProtectedRoute wraps pages"]
Protected --> Pages["Home/Setup/Report/SuperAdmin"]
ClearCtx --> LoginPage["Render Login"]
```

**Diagram sources**
- [App.js:43-94](file://bad-court-mana-ui/src/App.js#L43-L94)
- [AuthContext.js:14-50](file://bad-court-mana-ui/src/context/AuthContext.js#L14-L50)
- [index.js:10-18](file://bad-court-mana-ui/src/index.js#L10-L18)

**Section sources**
- [App.js:1-104](file://bad-court-mana-ui/src/App.js#L1-L104)
- [AuthContext.js:1-91](file://bad-court-mana-ui/src/context/AuthContext.js#L1-L91)
- [index.js:1-21](file://bad-court-mana-ui/src/index.js#L1-L21)

### Backend: CORS and CSRF Configuration
SecurityConfig configures CSRF and CORS for local development. Origins include both frontend (port 3000) and backend (port 8080) during development.

```mermaid
flowchart TD
Req["Incoming Request"] --> CSRF["CSRF Token Validation"]
Req --> CORS["CORS Origin Check"]
CSRF --> Allowed{"Allowed?"}
CORS --> Allowed
Allowed --> |Yes| Proceed["Proceed to Controller"]
Allowed --> |No| Deny["403/401 Response"]
```

**Diagram sources**
- [SecurityConfig.java:44-92](file://BadmintonCourtManagement/src/main/java/com/badminton/config/SecurityConfig.java#L44-L92)
- [SecurityConfig.java:112-140](file://BadmintonCourtManagement/src/main/java/com/badminton/config/SecurityConfig.java#L112-L140)

**Section sources**
- [SecurityConfig.java:112-140](file://BadmintonCourtManagement/src/main/java/com/badminton/config/SecurityConfig.java#L112-L140)

## Dependency Analysis
- Backend dependencies include Spring Web, Security, Data JPA, Liquibase, MySQL driver, Gson, validation, Apache POI, and Lombok with enhanced annotation processing support.
- Frontend dependencies include React 19, React Router v7, Axios, js-cookie, Bootstrap, React Bootstrap, Material-UI, and drag-and-drop libraries with env-cmd for environment management.

```mermaid
graph LR
subgraph "Backend Dependencies"
W["spring-boot-starter-web"]
S["spring-boot-starter-security"]
J["spring-boot-starter-data-jpa"]
L["liquibase-core"]
M["mysql-connector-j"]
G["gson"]
V["validation"]
P["poi-ooxml"]
LB["lombok<br/>Enhanced Annotation Processing"]
end
subgraph "Frontend Dependencies"
R["react 19 + react-dom 19"]
RR["react-router 7 + dom 7"]
AX["axios"]
JC["js-cookie"]
BS["bootstrap 5 + icons"]
RB["react-bootstrap 2.10"]
MU["@mui/material 9.0 + icons 9.0"]
DnD["react-beautiful-dnd 13.1 + react-dnd 16"]
ENV["env-cmd 11.0.0<br/>QA Environment Support"]
end
W --> S
W --> J
J --> M
W --> G
W --> V
W --> P
R --> RR
R --> AX
AX --> JC
R --> BS
R --> RB
R --> MU
R --> DnD
R --> ENV
```

**Diagram sources**
- [pom.xml:37-113](file://BadmintonCourtManagement/pom.xml#L37-L113)
- [package.json:6-29](file://bad-court-mana-ui/package.json#L6-L29)
- [package.json:55-57](file://bad-court-mana-ui/package.json#L55-L57)

**Section sources**
- [pom.xml:37-113](file://BadmintonCourtManagement/pom.xml#L37-L113)
- [package.json:6-29](file://bad-court-mana-ui/package.json#L6-L29)
- [package.json:55-57](file://bad-court-mana-ui/package.json#L55-L57)

## Performance Considerations
- Backend
  - Enable DevTools selectively for faster iteration; keep off in CI/production.
  - Use Liquibase migrations to manage schema changes efficiently.
  - Keep database queries optimized; leverage pagination for report endpoints.
  - Lombok annotation processing is now optimized across all Maven profiles for consistent build performance.
- Frontend
  - Minimize unnecessary re-renders; memoize heavy computations.
  - Lazy-load routes/components to improve initial load time.
  - Use Material-UI tree-shaking and avoid importing entire libraries.
  - Utilize build:qa script for environment-specific optimizations.

**Updated** Enhanced performance considerations now include Lombok annotation processing optimization and environment-specific build capabilities.

## Troubleshooting Guide
- JDK 21 verification and setup
  - Use the provided scripts to detect and configure JDK 21, JAVA_HOME, and Maven.
  - Verify versions and profile activation.
- Backend
  - Confirm active profile and context path in application properties.
  - Check CSRF token age and session timeout alignment.
  - Verify Lombok annotation processor configuration in Maven profiles.
- Frontend
  - Ensure cookies are readable and CSRF token is present.
  - Validate router configuration and protected route wrappers.
  - Check .env.qa file exists for build:qa command.
- Build System
  - Use copy_build_packages.sh for automated multi-environment builds.
  - Verify environment-specific artifact naming (dev/qa/prod).

**Section sources**
- [check-jdk21.sh:1-111](file://BadmintonCourtManagement/check-jdk21.sh#L1-L111)
- [setup-jdk21.sh:1-40](file://BadmintonCourtManagement/setup-jdk21.sh#L1-L40)
- [application.properties:1-19](file://BadmintonCourtManagement/src/main/resources/application.properties#L1-L19)
- [application-dev.properties:1-12](file://BadmintonCourtManagement/src/main/resources/application-dev.properties#L1-L12)
- [application-qa.properties:1-9](file://BadmintonCourtManagement/src/main/resources/application-qa.properties#L1-L9)
- [application-prod.properties:1-9](file://BadmintonCourtManagement/src/main/resources/application-prod.properties#L1-L9)
- [AuthenController.java:29-73](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/AuthenController.java#L29-L73)
- [AuthContext.js:14-32](file://bad-court-mana-ui/src/context/AuthContext.js#L14-L32)
- [copy_build_packages.sh:36-47](file://deployment/copy_build_packages.sh#L36-L47)

## Conclusion
This document outlines the development guidelines for the Badminton Court Management system, focusing on JDK 21 setup, enhanced Maven configuration with Lombok support, React SPA organization, security and authentication, and operational workflows. The recent updates include comprehensive Lombok annotation processor configuration across all Maven profiles, advanced frontend build capabilities with QA environment support, and automated deployment workflows. Following these standards ensures consistent development, reliable builds, secure integrations, and maintainable code across the monorepo.

## Appendices

### A. JDK 21 Setup Requirements
- Install JDK 21 (Temurin/OpenJDK/Oracle) and Maven 3.8+.
- Configure JAVA_HOME and PATH; verify with provided scripts.
- Use Maven wrapper for consistent builds across environments.
- Enhanced verification includes Lombok annotation processor detection.

**Updated** Comprehensive JDK 21 migration requirements with automated verification, setup scripts, and Lombok annotation processor configuration.

**Section sources**
- [JDK21_SETUP.md:1-142](file://BadmintonCourtManagement/JDK21_SETUP.md#L1-L142)
- [check-jdk21.sh:1-111](file://BadmintonCourtManagement/check-jdk21.sh#L1-L111)
- [setup-jdk21.sh:1-40](file://BadmintonCourtManagement/setup-jdk21.sh#L1-L40)

### B. Development Environment Configuration
- Backend
  - Profiles: dev (default), qa, prod; WAR names differ per profile with enhanced Lombok support.
  - Properties: currency, session timeout, Liquibase settings, environment-specific configurations.
- Frontend
  - Scripts: start, build, build:qa (new), test; build:qa uses env-cmd for QA environment.
  - Theming and routing via Material-UI and React Router.

**Updated** Enhanced frontend documentation standards with React 19, Material-UI 9.0, modern development practices, and new build:qa capability.

**Section sources**
- [pom.xml:115-222](file://BadmintonCourtManagement/pom.xml#L115-L222)
- [application.properties:1-19](file://BadmintonCourtManagement/src/main/resources/application.properties#L1-L19)
- [application-dev.properties:1-12](file://BadmintonCourtManagement/src/main/resources/application-dev.properties#L1-L12)
- [application-qa.properties:1-9](file://BadmintonCourtManagement/src/main/resources/application-qa.properties#L1-L9)
- [application-prod.properties:1-9](file://BadmintonCourtManagement/src/main/resources/application-prod.properties#L1-L9)
- [package.json:30-36](file://bad-court-mana-ui/package.json#L30-L36)
- [index.js:8-18](file://bad-court-mana-ui/src/index.js#L8-L18)

### C. Maven Project Structure and Build Optimization
- Properties define Java 21 compiler targets and release with consistent annotation processing.
- Profiles customize artifact names and plugin configurations with Lombok support.
- Enhanced optimization: parallel builds, dependency updates, minimal DevTools in CI, consistent annotation processing across all environments.
- Lombok annotation processors configured in all Maven profiles (dev, qa, prod).

**Updated** Enhanced Maven configuration now includes comprehensive Lombok annotation processor setup across all build profiles.

**Section sources**
- [pom.xml:31-36](file://BadmintonCourtManagement/pom.xml#L31-L36)
- [pom.xml:115-222](file://BadmintonCourtManagement/pom.xml#L115-L222)

### D. React Development Guidelines
- Component organization: page/, dialog/, dragNdrop/, context/.
- Theming: Material-UI ThemeProvider and CssBaseline.
- Routing: React Router v7 with protected routes and lazy loading.
- State management: Context API for authentication and global state.
- Build system: Enhanced with build:qa script using env-cmd for environment-specific deployments.

**Updated** Enhanced frontend documentation standards with React 19, Material-UI 9.0, modern development practices, and advanced build automation.

**Section sources**
- [App.js:1-104](file://bad-court-mana-ui/src/App.js#L1-L104)
- [AuthContext.js:1-91](file://bad-court-mana-ui/src/context/AuthContext.js#L1-L91)
- [index.js:1-21](file://bad-court-mana-ui/src/index.js#L1-L21)
- [package.json:1-59](file://bad-court-mana-ui/package.json#L1-L59)

### E. Coding Standards and Naming Conventions
- Java
  - Package naming: com.badminton.*
  - Controllers: @RestController, clear HTTP verb mapping, consistent DTO usage.
  - Services: interface + impl separation, calculator packages for domain logic.
  - Entities: JPA annotations, naming aligned with DB schema, enhanced with Lombok annotations.
  - Exceptions: centralized GlobalExceptionHandler, Business/ElementNotExist exceptions.
- JavaScript/React
  - Components: PascalCase, folder-per-feature under src/page and src/dialog.
  - Hooks/state: centralized in Context providers.
  - Styling: Material-UI components, consistent spacing and theme.
  - Environment management: env-cmd for QA environment configuration.

**Updated** Enhanced coding standards with JDK 21 requirements, Lombok annotation support, and modern React patterns.

**Section sources**
- [AuthenController.java:1-111](file://BadmintonCourtManagement/src/main/java/com/badminton/controller/AuthenController.java#L1-L111)
- [SecurityConfig.java:1-193](file://BadmintonCourtManagement/src/main/java/com/badminton/config/SecurityConfig.java#L1-L193)
- [App.js:1-104](file://bad-court-mana-ui/src/App.js#L1-L104)
- [AuthContext.js:1-91](file://bad-court-mana-ui/src/context/AuthContext.js#L1-L91)

### F. Code Review and Branching Workflow
- Branching: feature branches merged via pull requests with reviewers.
- Code review: enforce naming conventions, security (CSRF/CORS), Lombok annotation usage, and test coverage.
- CI: build with Maven wrapper, run tests, linting, dependency checks, and environment-specific builds.
- Build optimization: utilize enhanced Maven profiles and build:qa capability for QA deployments.

### G. Development Lifecycle: From Feature to Deployment
- Feature implementation: implement backend endpoints/controllers, React components/pages, and DTOs with Lombok support.
- Testing: unit tests for services, integration tests for controllers, frontend component tests.
- Build: Maven profiles for dev/qa/prod with enhanced Lombok annotation processing; React build for production with build:qa for QA.
- Deployment: automated package collection via copy_build_packages.sh for environment-specific artifacts; ensure Liquibase migrations run.

**Updated** Enhanced deployment workflow now includes automated environment-specific builds with Lombok support and advanced frontend build capabilities.

**Section sources**
- [README.md:105-126](file://README.md#L105-L126)
- [pom.xml:115-222](file://BadmintonCourtManagement/pom.xml#L115-L222)
- [package.json:30-36](file://bad-court-mana-ui/package.json#L30-L36)
- [copy_build_packages.sh:36-47](file://deployment/copy_build_packages.sh#L36-L47)