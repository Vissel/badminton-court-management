# Getting Started

<cite>
**Referenced Files in This Document**
- [README.md](file://README.md)
- [JDK21_SETUP.md](file://JDK21_SETUP.md)
- [pom.xml](file://BadmintonCourtManagement/pom.xml)
- [application.properties](file://BadmintonCourtManagement/src/main/resources/application.properties)
- [application-dev.properties](file://BadmintonCourtManagement/src/main/resources/application-dev.properties)
- [application-qa.properties](file://BadmintonCourtManagement/src/main/resources/application-qa.properties)
- [application-prod.properties](file://BadmintonCourtManagement/src/main/resources/application-prod.properties)
- [db.changelog-master.xml](file://BadmintonCourtManagement/src/main/resources/db/changelog/db.changelog-master.xml)
- [SecurityConfig.java](file://BadmintonCourtManagement/src/main/java/com/badminton/config/SecurityConfig.java)
- [BadmintonCourtManagementApplication.java](file://BadmintonCourtManagement/src/main/java/com/badminton/BadmintonCourtManagementApplication.java)
- [package.json](file://bad-court-mana-ui/package.json)
- [index.html](file://bad-court-mana-ui/public/index.html)
- [deployment-note.txt](file://deployment/deployment-note.txt)
- [pre-deployment.sh](file://deployment/pre-deployment.sh)
</cite>

## Table of Contents
1. [Introduction](#introduction)
2. [Prerequisites](#prerequisites)
3. [Development Environment Setup](#development-environment-setup)
4. [Backend (Spring Boot) Setup](#backend-spring-boot-setup)
5. [Frontend (React) Setup](#frontend-react-setup)
6. [Database Configuration with Liquibase](#database-configuration-with-liquibase)
7. [Environment-Specific Properties](#environment-specific-properties)
8. [Build and Deployment](#build-and-deployment)
9. [Monorepo Structure](#monorepo-structure)
10. [CORS Configuration for Development](#cors-configuration-for-development)
11. [Deployment Topology with Tomcat](#deployment-topology-with-tomcat)
12. [Verification Steps](#verification-steps)
13. [Troubleshooting Guide](#troubleshooting-guide)
14. [Conclusion](#conclusion)

## Introduction
This guide helps you set up and run the Badminton Court Management system locally and deploy it to production. The system consists of:
- Backend: Spring Boot 3.5.x (Java 21) with WAR packaging for Tomcat
- Frontend: React 19 SPA served from a subpath
- Database: MySQL with Liquibase migrations
- Profiles: dev, qa, prod for different environments

## Prerequisites
Ensure the following tools are installed and available on your machine:
- Java 21 (required)
- Maven 3.8+ (Maven Wrapper included)
- Node.js and npm (for frontend)
- MySQL server

Key references:
- Backend runtime and framework requirements
- Java 21 requirement and Maven configuration
- Frontend stack and build scripts

**Section sources**
- [README.md: 54-86:54-86](file://README.md#L54-L86)
- [JDK21_SETUP.md: 3-10:3-10](file://JDK21_SETUP.md#L3-L10)
- [pom.xml: 31-36:31-36](file://BadmintonCourtManagement/pom.xml#L31-L36)
- [package.json: 1-59:1-59](file://bad-court-mana-ui/package.json#L1-L59)

## Development Environment Setup
Follow these steps to prepare your environment:

1. Install and verify Java 21
   - Use SDKMAN!, Homebrew, or manual installation as described in the JDK setup guide.
   - Confirm versions with the provided commands.

2. Install Maven
   - Use the Maven wrapper included in the repository or install Maven 3.8+.

3. Install Node.js and npm
   - Ensure Node.js and npm are installed for the frontend.

4. Install MySQL
   - Install MySQL server and ensure it is running.

References:
- JDK 21 setup and verification
- Maven wrapper usage and profile builds
- Frontend dependencies and scripts

**Section sources**
- [JDK21_SETUP.md: 11-50:11-50](file://JDK21_SETUP.md#L11-L50)
- [JDK21_SETUP.md: 75-98:75-98](file://JDK21_SETUP.md#L75-L98)
- [package.json: 30-36:30-36](file://bad-court-mana-ui/package.json#L30-L36)

## Backend (Spring Boot) Setup
The backend is a Spring Boot 3.5.x application packaged as a WAR for Tomcat. It uses Java 21 and supports Maven profiles for environments.

Steps:
1. Navigate to the backend module directory.
2. Build with Maven using the desired profile:
   - Development: `-Pdev`
   - QA: `-Pqa`
   - Production: `-Pprod`
3. The WAR artifact is generated in the backend target directory with a name based on the active profile.

References:
- Backend stack and WAR packaging
- Maven profiles and final names
- Profile activation and properties

**Section sources**
- [README.md: 56-70:56-70](file://README.md#L56-L70)
- [pom.xml: 115-201:115-201](file://BadmintonCourtManagement/pom.xml#L115-L201)
- [application.properties: 1-19:1-19](file://BadmintonCourtManagement/src/main/resources/application.properties#L1-L19)

## Frontend (React) Setup
The frontend is a React 19 application built with Create React App. It is configured to run on port 3000 and to be hosted under a subpath.

Steps:
1. Navigate to the frontend directory.
2. Install dependencies using npm.
3. Start the development server or build for production.

References:
- React version and homepage configuration
- Build scripts and environment-specific build command
- Public HTML structure for subpath hosting

**Section sources**
- [README.md: 71-81:71-81](file://README.md#L71-L81)
- [package.json: 5, 30-36, 55-58:5-58](file://bad-court-mana-ui/package.json#L5-L58)
- [index.html: 1-44:1-44](file://bad-court-mana-ui/public/index.html#L1-L44)

## Database Configuration with Liquibase
The system uses Liquibase for database schema management. The master changelog references SQL files for initial setup.

Steps:
1. Create the target database(s) for each environment (dev, qa, prod).
2. Ensure the MySQL user has appropriate privileges.
3. Liquibase is configured in the environment-specific properties files to load the master changelog.

References:
- Liquibase configuration in properties
- Master changelog and referenced SQL files
- Database creation and user setup guidance

**Section sources**
- [application-dev.properties: 10-11:10-11](file://BadmintonCourtManagement/src/main/resources/application-dev.properties#L10-L11)
- [application-qa.properties: 8-9:8-9](file://BadmintonCourtManagement/src/main/resources/application-qa.properties#L8-L9)
- [application-prod.properties: 8-9:8-9](file://BadmintonCourtManagement/src/main/resources/application-prod.properties#L8-L9)
- [db.changelog-master.xml: 7-15:7-15](file://BadmintonCourtManagement/src/main/resources/db/changelog/db.changelog-master.xml#L7-L15)

## Environment-Specific Properties
Environment-specific properties define database connections, Liquibase settings, and server context paths.

- Development (dev)
  - Context path and port
  - MySQL connection details
  - Liquibase enabled flag and change log path

- QA
  - Different context path and database
  - Liquibase enabled flag and change log path

- Production (prod)
  - Production context path and database
  - Liquibase enabled flag and change log path

References:
- Active profile property
- Environment-specific property files

**Section sources**
- [application.properties: 1](file://BadmintonCourtManagement/src/main/resources/application.properties#L1)
- [application-dev.properties: 1-11:1-11](file://BadmintonCourtManagement/src/main/resources/application-dev.properties#L1-L11)
- [application-qa.properties: 1-9:1-9](file://BadmintonCourtManagement/src/main/resources/application-qa.properties#L1-L9)
- [application-prod.properties: 1-9:1-9](file://BadmintonCourtManagement/src/main/resources/application-prod.properties#L1-L9)

## Build and Deployment
Build and deployment instructions vary by environment.

Backend:
- Use Maven profiles to build WAR artifacts:
  - Development: `-Pdev`
  - QA: `-Pqa`
  - Production: `-Pprod`
- The generated WAR files are named according to the active profile.

Frontend:
- Install dependencies and build:
  - Development: `npm start`
  - Production: `npm run build`
  - QA build variant: `npm run build:qa`

References:
- Backend build commands and profile names
- Frontend build scripts and QA build command

**Section sources**
- [README.md: 105-124:105-124](file://README.md#L105-L124)
- [pom.xml: 115-201:115-201](file://BadmintonCourtManagement/pom.xml#L115-L201)
- [package.json: 30-36:30-36](file://bad-court-mana-ui/package.json#L30-L36)

## Monorepo Structure
The repository follows a monorepo-style layout with two top-level areas:
- Backend module: Spring Boot application under BadmintonCourtManagement
- Frontend module: React SPA under bad-court-mana-ui
- Deployment assets and documentation under deployment and docs

References:
- High-level architecture overview mentioning monorepo layout

**Section sources**
- [README.md: 89-95:89-95](file://README.md#L89-L95)

## CORS Configuration for Development
During development, the backend enables CORS to allow requests from the frontend running on port 3000 and the backend running on port 8080. Credentials are allowed, and common HTTP methods and headers are permitted.

References:
- CORS configuration bean and allowed origins
- Development-time CORS policy

**Section sources**
- [SecurityConfig.java: 112-136:112-136](file://BadmintonCourtManagement/src/main/java/com/badminton/config/SecurityConfig.java#L112-L136)

## Deployment Topology with Tomcat
Production deployment uses Apache Tomcat with the following topology:
- Frontend static assets deployed under a subpath (e.g., caulong-tc)
- Backend deployed as a WAR under a subpath (e.g., bad-court-management)
- Database configured per environment

Deployment steps:
1. Build the frontend and backend for production.
2. Prepare the deployment package using the provided script or manually collect the frontend folder and backend WAR.
3. Deploy the frontend folder to the static hosting location or Tomcat web root.
4. Deploy the backend WAR to Tomcat webapps.
5. Start Tomcat.

References:
- Production build and packaging notes
- Pre-deployment script for collecting packages
- Tomcat deployment and startup guidance

**Section sources**
- [deployment-note.txt: 1-113:1-113](file://deployment/deployment-note.txt#L1-L113)
- [pre-deployment.sh: 1-41:1-41](file://deployment/pre-deployment.sh#L1-L41)

## Verification Steps
To verify a successful installation and configuration:

1. Backend
   - Confirm the active profile and context path from properties.
   - Start the backend using the chosen profile and check logs for successful initialization.
   - Access the API endpoints defined in the backend.

2. Database
   - Verify the target database exists and is accessible with the configured credentials.
   - Confirm Liquibase ran and applied the master changelog.

3. Frontend
   - Start the frontend and confirm it runs on the expected port.
   - Ensure API calls reach the backend through the configured base URL.

4. Cross-Origin Requests
   - During development, verify that requests from the frontend to the backend succeed without CORS errors.

References:
- Application properties and active profile
- Backend main application class
- Security configuration for CORS and CSRF
- Frontend homepage and build scripts

**Section sources**
- [application.properties: 1-19:1-19](file://BadmintonCourtManagement/src/main/resources/application.properties#L1-L19)
- [BadmintonCourtManagementApplication.java: 7-15:7-15](file://BadmintonCourtManagement/src/main/java/com/badminton/BadmintonCourtManagementApplication.java#L7-L15)
- [SecurityConfig.java: 44-92:44-92](file://BadmintonCourtManagement/src/main/java/com/badminton/config/SecurityConfig.java#L44-L92)
- [package.json: 5, 30-36:5-36](file://bad-court-mana-ui/package.json#L5-L36)

## Troubleshooting Guide
Common issues and resolutions:

- Wrong JDK Version
  - Symptom: Compilation errors or runtime compatibility issues.
  - Resolution: Ensure JDK 21 is selected and visible to Maven and the IDE.

- Maven Using Wrong JDK
  - Symptom: Maven reports a different Java version than expected.
  - Resolution: Set JAVA_HOME to the JDK 21 installation or configure Maven accordingly.

- IDE Issues
  - Symptom: IDE warnings about bytecode level or missing SDK.
  - Resolution: Configure the project SDK and language level to Java 21 in your IDE.

- Database Connectivity
  - Symptom: Cannot connect to MySQL or Liquibase fails.
  - Resolution: Verify database credentials, network connectivity, and that the database exists. Confirm Liquibase change log path and permissions.

- CORS Errors in Development
  - Symptom: Browser blocks cross-origin requests between frontend and backend.
  - Resolution: Confirm CORS configuration allows the frontend origin and that credentials are permitted.

- Tomcat Startup and Path Issues
  - Symptom: Application not reachable or static assets not loading.
  - Resolution: Ensure the frontend folder and backend WAR are deployed under the correct subpaths and that Tomcat is started with Java 21.

References:
- JDK setup troubleshooting
- Maven and IDE configuration tips
- Database and Liquibase configuration
- CORS configuration
- Tomcat deployment notes

**Section sources**
- [JDK21_SETUP.md: 100-142:100-142](file://JDK21_SETUP.md#L100-L142)
- [application-dev.properties: 5-11:5-11](file://BadmintonCourtManagement/src/main/resources/application-dev.properties#L5-L11)
- [SecurityConfig.java: 112-136:112-136](file://BadmintonCourtManagement/src/main/java/com/badminton/config/SecurityConfig.java#L112-L136)
- [deployment-note.txt: 88-113:88-113](file://deployment/deployment-note.txt#L88-L113)

## Conclusion
You now have the essentials to set up the Badminton Court Management system locally, configure environments, build both backend and frontend, and deploy to Tomcat. Use the provided references to validate your setup and troubleshoot common issues.