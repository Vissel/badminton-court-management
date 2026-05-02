# Badminton Court Management (TC)

English overview of the **Badminton Court Management** system for operating a badminton venue day-to-day: sessions, courts, doubles lineups, shuttles, add-on services, match results, per-player costs, payments, and Excel reporting. The Vietnamese end-user guide is in `docs/Huong_dan_su_dung_He thong quan ly san cau long TC.docx`.

---

## Purpose

Help **court administrators** and **cashiers** run the facility efficiently: one or more **working sessions per day**, live court and match state, accurate **per-player charges** (court fee, shuttles, drinks), and **audit-friendly exports** for revenue review.

---

## Use cases and how they compare to the market

| Use case (this product) | Typical “market” badminton software | Notes |
|--------|-------------------------------------|--------|
| **Daily operational session** (open/close venue for the day, possibly multiple times) | Often **online booking** or **membership** first | This system is **front-desk / floor operations** oriented, not consumer self-booking. |
| **Player check-in** to the current session, reuse players from DB, dedupe by session | CRM-lite or walk-in lists | Strong focus on **same-day presence** and **billing**, not long-term CRM pipelines. |
| **Court layout**: positions A–B (team 1) vs C–D (team 2), drag-and-drop | Simple “book court 3–4pm” grids | Aligns with **real doubles court geography** and **staff-controlled** assignments. |
| **Match lifecycle**: Not started → In progress → Finished (with result & cost split) or Cancelled | Booking-only apps lack match economics | **Cost allocation** and **winner** capture at end of match. |
| **Shuttle usage per match** + **extra services** (e.g. water) per player | May be invoice-only or POS separate | **Integrated** consumables and services on the **same operational screen**. |
| **Payment & leave session** with method and timestamp | Stripe/subscription models | Suited to **cash / local payment** workflows and **clear session exit**. |
| **Owner settings**: courts, shuttle types, services, default per-head court fee | Multi-tenant SaaS pricing tiers | **Venue configuration** in-app; documented as best changed **before** opening the day’s session. |
| **Reports**: sessions by month, revenue, Excel per session or bulk | BI dashboards, cloud analytics | **Practical Excel** exports for accountants and owners. |
| **Security**: login required; session / CSRF token lifetime (~30 min per product docs) | OAuth, magic links | **Classic session-based** staff login; re-login does not lose persisted data. |

**Positioning:** A **specialized venue operations and settlement** tool rather than a public booking marketplace or league management platform.

---

## User roles (from product documentation)

| Role | Responsibilities |
|------|------------------|
| **Court owner** | Configure courts, shuttle types, add-on services, and prices; default per-player court fee. |
| **Administrator / cashier** | Login, sessions, players, courts, matches, shuttles/services, results, costs, payments, reports. |
| **Internal super admin** | Create admin accounts, reset passwords (exceptional / internal use). |

---

## Key features

1. **Authentication** — All actions after successful login; documented **30-minute** working session behavior; logout/re-login within the day does not discard saved data.
2. **Daily sessions** — Automatic handling when the first login of a new day closes the previous day’s session and starts a new one; **close venue** ends the session, cancels unfinished matches, marks remaining players as left.
3. **Players in session** — Add by name; **default court fee** applied; reuse existing DB player; normalize case on name match; reject duplicate if still in session.
4. **Court & match management** — Positions **A, B** (team 1) and **C, D** (team 2); **Start** only when each team has at least one player; remove players from court only while match **not started** (otherwise cancel match and re-assign).
5. **Match states** — Not started → in progress (with **End** / **Cancel**) → finished with **result dialog** (winning team, adjustable amounts, validation that totals reconcile) or cancelled.
6. **Shuttles & services** — Add shuttle types to a match; drag-and-drop **services** onto players; services stored on the player record for the current session.
7. **Payment** — Confirm totals, payment method/status, time left; marks end of play for that person; adjust services before payment if needed.
8. **Reporting** — List sessions (code, date, time range, aggregate revenue); months with data; **Excel** export for one session or for **all filtered rows**; continuous saves reduce data loss on power loss; **reload** restores server state.

---

## Technical stack

### Backend (`BadmintonCourtManagement`)

| Area | Technology |
|------|------------|
| Runtime | **Java 21** |
| Framework | **Spring Boot 3.5.x** (WAR packaging for external Tomcat) |
| Security | **Spring Security**, BCrypt passwords, session + CSRF (configurable) |
| Data | **Spring Data JPA**, **MySQL** (`mysql-connector-j`) |
| Migrations | **Liquibase** (`db/changelog`) |
| API | REST controllers (`/login`, `/session`, `/court-mana`, `/gameResult`, `/api/v1/pay`, `/api/v1/manager`, `/admin/internal`, `/api` settings, etc.) |
| Documents | **Apache POI** (Excel export) |
| JSON | **Gson** |
| Validation | `spring-boot-starter-validation` |
| Other | Lombok, `@EnableScheduling`, optional DevTools |

### Frontend (`bad-court-mana-ui`)

| Area | Technology |
|------|------------|
| UI | **React 19**, **Create React App** (`react-scripts`) |
| Routing | **React Router** v7 |
| HTTP | **Axios**, cookies (`js-cookie`) |
| Styling / UX | **Bootstrap 5**, **React Bootstrap**, **Bootstrap Icons** |
| Drag-and-drop | **react-beautiful-dnd**, **react-dnd** + HTML5 backend |
| Deploy path | `homepage`: `/caulong-tc/` (builds for subpath hosting) |

### Operations

- Maven profiles: **dev** (default), **qa**, **prod** — different WAR names (`bad-court-management-dev`, `bad-court-management-qa`, `bad-court-management`).
- Example packaged artifacts under `deployment/` (WAR/ZIP).

---

## Architecture (high level)

- **Monorepo-style layout:** Java API + separate React SPA; SPA talks to API (CORS configured for local dev: React on port 3000, Tomcat on 8080).
- **Session-centric domain:** A “session” is the business day (or shift) boundary for players, courts, games, and payments.
- **Reporting:** Paginated report list API; single-session export; multi-session export via token + **streaming** download for large exports (see `ManagerController`).

---

## Configuration highlights

- **Currency:** `VND` in `application.properties`.
- **Session timeout (CSRF / client guidance):** `session.timeout=1800000` ms (30 minutes), aligned with the user guide.
- **Profiles:** `application-dev.properties`, `application-qa.properties`, `application-prod.properties` for environment-specific DB and URLs.

---

## Building and running (summary)

**Backend**

```bash
cd BadmintonCourtManagement
mvn -Pdev clean package   # or -Pqa / -Pprod
```

Deploy the generated WAR to Tomcat (or run with Spring Boot depending on your setup).

**Frontend**

```bash
cd bad-court-mana-ui
npm install
npm start                 # development
npm run build             # production build
npm run build:qa          # QA env file via env-cmd
```

---

## Documentation

- **Vietnamese user manual:** `docs/Huong_dan_su_dung_He thong quan ly san cau long TC.docx` — workflows, screens, and business rules (session close, match validity, payment notes).

---

## License

Not specified in the repository; add a `LICENSE` file if you distribute the project.
