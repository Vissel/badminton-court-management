# Badminton Court Management System - Frontend

## Project Overview

Badminton Court Management is a specialized venue operations and settlement tool designed for front-desk staff who manage daily badminton court activities. It focuses on session-centric workflows, real-time court and match state tracking, integrated consumption tracking (shuttles, drinks), per-player cost calculation, and practical Excel reporting for revenue reconciliation. The system is intentionally positioned as a floor operations tool rather than an online booking or membership platform, emphasizing live check-in, match lifecycle management, and cash-oriented payments.

Key value propositions:
- Daily session boundary ensures accurate billing and audit trails.
- Integrated court layout with drag-and-drop player assignment aligns with doubles court geography.
- Match lifecycle captures start, end, cancellation, and result confirmation with cost allocation.
- Built-in Excel export supports owner and accountant workflows.
- Classic session-based authentication with CSRF protection suited for in-venue staff.

## Technology Stack

**Frontend:**
- React 19
- Create React App
- React Router v7
- Material-UI (@mui/material, @mui/icons-material, @emotion/react, @emotion/styled)
- Axios for HTTP client
- js-cookie for cookie management
- react-beautiful-dnd and react-dnd for drag-and-drop
- Bootstrap 5 and React Bootstrap

**Backend:**
- Java 21
- Spring Boot 3.5.x (WAR packaging for Tomcat)
- Spring Security with CSRF protection
- Spring Data JPA
- MySQL with Liquibase migrations
- Apache POI for Excel export
- Maven profiles: dev, qa, prod

## Frontend Architecture

### Component Organization

```
bad-court-mana-ui/
├── public/               # Static assets and entry HTML
├── src/
│   ├── api/              # API configuration and Axios client
│   │   ├── config.js     # Base URL and timeout configuration
│   │   └── index.js      # Axios instance with interceptors
│   ├── context/          # React Context providers
│   │   ├── AuthContext.js   # Authentication state management
│   │   ├── ProtectedRoute.js # Route guards
│   │   └── authRef.js       # Auth reference for logout
│   ├── page/             # Feature pages
│   │   ├── dragNdrop/    # Drag-and-drop components
│   │   ├── dialog/       # Dialog components
│   │   ├── HomePage.js   # Main court management interface
│   │   ├── ReportPage.js # Reporting and export
│   │   ├── SetupPage.js  # Services and shuttle configuration
│   │   ├── SuperAdminPage.js # Admin user management
│   │   └── LoginPage.js  # Authentication form
│   ├── theme.js          # Material-UI theme configuration
│   ├── App.js            # Routing and layout
│   └── index.js          # Application bootstrap
└── package.json          # Dependencies and scripts
```

### Core Components

- **Authentication Provider**: Centralizes session validation, CSRF token handling, and logout actions
- **Protected Route**: Guards routes by checking authentication state
- **API Client**: Axios instance with credentials, CSRF interceptors, and centralized error handling
- **HomePage**: Drag-and-drop arena for managing players, services, and games across courts
- **ReportPage**: Paginated reporting with filtering, sorting, and Excel export
- **SetupPage**: Administrative configuration of services, shuttle balls, and pricing
- **SuperAdminPage**: User registration and password reset flows
- **LoginPage**: Form-based authentication with CSRF propagation

### Routing and Protected Routes

The React app uses HashRouter with React Router v7 and ProtectedRoute guards. AuthContext initializes session validation on mount, retrieves CSRF token, and stores it in session storage.

### API Integration Layer

- Centralized configuration reads base URL from environment variables
- Axios instance enables credentials and attaches X-XSRF-TOKEN from session storage
- Response interceptor handles:
  - Network errors with user feedback
  - Forces logout on 401/403 outside excluded paths
  - Parses blob errors for exports
  - Surfaces generic 500 and client errors

### Drag-and-Drop Implementation

HomePage uses react-dnd with HTML5 backend for:
- Draggable services and player areas
- Court visualization with 2x2 grid (positions A-D)
- Real-time player movement and service assignment
- Interactive game controls (start, finish, cancel)

## Project Structure (Monorepo)

The repository follows a monorepo-style layout:

- **Backend**: `BadmintonCourtManagement/` - Spring Boot 3.5.x application
- **Frontend**: `bad-court-mana-ui/` - React 19 SPA
- **Deployment**: `deployment/` - Scripts and assets
- **Documentation**: `docs/` and root-level docs

## Available Scripts

In the `bad-court-mana-ui` directory, you can run:

### `npm start`

Runs the app in development mode.\
Open [http://localhost:3000](http://localhost:3000) to view it in your browser.

### `npm test`

Launches the test runner in interactive watch mode.

### `npm run build`

Builds the app for production to the `build` folder.

### `npm run build:qa`

Builds the app for QA environment.

## Build and Deployment

### Frontend Build

```bash
cd bad-court-mana-ui
npm install
npm run build    # Production
npm run build:qa # QA build
```

### Backend Build

```bash
cd BadmintonCourtManagement
mvn -Pdev clean package    # Development
mvn -Pqa clean package     # QA
mvn -Pprod clean package   # Production
```

### Deployment Topology

- Frontend static assets deployed under a subpath (e.g., `/caulong-tc/`)
- Backend deployed as WAR under a subpath (e.g., `/bad-court-management`)
- Apache Tomcat serves both components

## API Reference

### Authentication Endpoints
- `POST /login` - Authenticate user
- `POST /logout` - Terminate session
- `GET /csrf` - Validate/refresh CSRF token

### Court Management (`/court-mana/*`)
- `GET /court-mana/getAllActiveCourt` - Get active courts
- `GET /court-mana/getCourtManagement` - Get aggregated court data
- `GET /court-mana/getAvailablePlayers` - Get available players
- `GET /court-mana/getServices` - Get active services
- `GET /court-mana/getShuttleBalls` - Get active shuttle balls
- `POST /court-mana/addPlayer` - Add player to session
- `POST /court-mana/addPlayerToCourt` - Assign player to court area
- `POST /court-mana/removePlayerFromCourt` - Remove player from court
- `POST /court-mana/changeGameState` - Start/finish/cancel match
- `POST /court-mana/addServiceToPlayer` - Add service to player
- `POST /court-mana/updateServiceToPlayer` - Update player services
- `POST /court-mana/changeBallQuantity` - Change shuttle ball quantity
- `POST /court-mana/changeSelectedBall` - Set selected shuttle ball

### Game Results (`/gameResult/*`)
- `GET /gameResult/getGameResult` - Get game result for court
- `POST /gameResult/confirmGameResult` - Confirm result
- `POST /gameResult/rejectGameResult` - Reject/terminate game

### Payments (`/api/v1/pay/*`)
- `POST /api/v1/pay/payToPlayer` - Process payment

### Reporting (`/api/v1/manager/*`)
- `POST /api/v1/manager/reportList` - Paginated report listing
- `GET /api/v1/manager/reportExport/{sessionId}` - Single session export
- `GET /api/v1/manager/stream/reportExportList/{token}` - Bulk streaming export
- `GET /api/v1/manager/getMonthYear` - Month/year filter options
- `POST /api/v1/manager/reportToken` - Prepare bulk export token

### Settings (`/api/*`)
- `GET /api/getSetupServices` - Get current configuration
- `POST /api/addSetupService` - Add new services/shuttles
- `POST /api/updateSetupService` - Update existing configuration
- `PUT /api/deleteService` - Delete service
- `PUT /api/deleteShuttleBall` - Delete shuttle ball

## Security Model

### Authentication and CSRF
- Session-based authentication with 30-minute timeout
- Cookie-based CSRF token repository
- X-XSRF-TOKEN header required for authenticated requests
- CORS configured for localhost:3000 (frontend) and localhost:8080 (Tomcat)

### Protected Routes
- All routes except login/logout/csrf require authentication
- ProtectedRoute wrapper redirects unauthenticated users to login
- Axios interceptor forces logout on 401/403 responses

## Data Model

### Core Entities
- **Session**: Daily operational boundary with available players
- **Player**: Registered users in the system
- **AvailablePlayer**: Player presence during a session
- **Court**: Facility hosting matches
- **Game**: Match lifecycle with teams and shuttle usage
- **Team**: Player pairings with expenses and outcomes
- **ShuttleBall**: Equipment tracked per game
- **GameShuttleMap**: Shuttle-ball usage quantities per game

### Entity Relationships
- Session 1:N AvailablePlayer
- Player 1:N AvailablePlayer
- Court 1:N Game
- Game 1:2 Team (teamOne/teamTwo)
- Game 1:N GameShuttleMap
- GameShuttleMap 1:1 ShuttleBall

## Development Guidelines

### Prerequisites
- Node.js and npm
- Java 21 (for full-stack development)
- Maven 3.8+ (for backend)
- MySQL server

### Local Development Setup

1. **Backend Setup**:
   ```bash
   cd BadmintonCourtManagement
   mvn -Pdev spring-boot:run
   ```

2. **Frontend Setup**:
   ```bash
   cd bad-court-mana-ui
   npm install
   npm start
   ```

3. **CORS Configuration**: Backend allows requests from `localhost:3000` during development

### Code Standards

**JavaScript/React**:
- Components: PascalCase
- Folder-per-feature under `src/page` and `src/dialog`
- Hooks/state centralized in Context providers
- Material-UI components with consistent theme

**Java**:
- Package naming: `com.badminton.*`
- Controllers: @RestController with clear HTTP verb mapping
- Services: Interface + implementation pattern
- Entities: JPA annotations aligned with DB schema

## Testing Strategy

- Service layer tests with @SpringBootTest
- Repository tests for JPQL queries
- Controller tests for HTTP endpoints
- Integration tests for end-to-end workflows
- JUnit 5, Mockito for mocking

## Environment Configuration

### Frontend Environment Variables
- `REACT_APP_API_BASE_URL` - Backend API base URL
- Build scripts support dev, qa, and prod environments

### Backend Profiles
- **dev**: Development with local database, Liquibase enabled
- **qa**: QA environment with separate database
- **prod**: Production configuration

## Troubleshooting

### Common Issues

**Authentication issues**:
- Verify CSRF token presence and session validity
- Use forceLogout to clear stale sessions

**API errors**:
- Network errors: Confirm backend availability and CORS
- 401/403: Ensure non-excluded paths trigger logout

**Drag-and-drop issues**:
- Ensure DndProvider is present at page level
- Validate draggable types and drop zones

**CORS errors**:
- Confirm frontend runs on localhost:3000
- Verify backend CORS configuration allows credentials

## Build Configuration

### Subpath Hosting
The `homepage` field in `package.json` configures the app to serve under a subpath for production deployment.

### Environment-Specific Builds
Additional script targets support QA builds using `env-cmd`.

## Deployment Notes

- Frontend builds to `build/` directory
- Backend produces WAR artifacts in `target/`
- Deployment scripts package both components for Tomcat
- Context paths must match between frontend `homepage` and backend WAR name

## Support

For issues and questions:
- Report issues via the repository issue tracker
- Visit the project documentation for detailed guides
