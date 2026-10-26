# Student Issue Management and Prioritization System

## ⚡ Quick Start (Windows / Linux / macOS / IDE)

1. Make sure **Java 21** is installed (`java -version`).
2. Put your Supabase values in a file named **`.env`** next to `pom.xml` (copy `.env.example`). Do not use quotes:
   ```
   SUPABASE_DB_URL=jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres?sslmode=require
   SUPABASE_DB_USERNAME=postgres.<your-project-ref>
   SUPABASE_DB_PASSWORD=<your-password>
   ```
3. Run the app from the project folder:
   - Windows: `.\mvnw.cmd spring-boot:run`
   - Linux / macOS: `./mvnw spring-boot:run`
   - IntelliJ / Eclipse / VS Code: just run `StudentIssueManagementApplication` (working directory = project folder).
4. Open **http://localhost:8080**

That's it. The app **reads `.env` automatically** (`spring.config.import` in `application.properties`) — no PowerShell or `source .env` commands are needed any more — and it **creates the tables and the `issue_summary` view automatically** on first start (`src/main/resources/db/schema-postgres.sql`, safe to re-run, never deletes data).

Run the tests (no database needed, uses in-memory H2): `.\mvnw.cmd test` (Windows) or `./mvnw test`.


A full-stack college web application built with **Spring Boot 3**, **Spring Data JPA**, **Hibernate**, **Thymeleaf**, and **PostgreSQL (hosted on Supabase)**.

This system empowers students to report campus issues, upvote critical concerns to raise their priority, and confirm resolutions through collective consensus.

---

## 🏛️ System Features & Business Logic

1. **Issue Reporting**: Students can report new campus problems with title, category, location, and description.
2. **Issue Listing & Viewing**: Browse all reported issues with real-time metadata, vote counts, confirmation status, and priority indicators.
3. **Multi-Field Search**: Real-time filtering across issue **title**, **description**, **category**, and **location**.
4. **Community Voting**: Students can upvote existing issues to elevate their visibility.
5. **Dynamic Sorting**: Instant toggling between **Most Votes** (`🔥`) and **Newest** (`🕒`).
6. **Automatic Priority Calculation**:
   - **0 – 9 Votes**: `LOW` Priority (Emerald badge)
   - **10 – 19 Votes**: `MEDIUM` Priority (Amber badge)
   - **20+ Votes**: `HIGH` Priority (Rose badge with pulse animation)
7. **Browser Identifier (UUID) Tracking**:
   - Every browser receives a unique UUID generated and stored in `localStorage`.
   - The backend validates this UUID to **strictly prevent duplicate votes** from the same browser identifier.
8. **Consensus-Driven Resolution Confirmation**:
   - Students can independently confirm that a reported issue has been resolved.
   - Once **5 distinct browser identifiers** confirm resolution, the issue status automatically flips from `OPEN` to `RESOLVED`.
9. **Zero Authentication Overhead**:
   - No login screens, passwords, or administrator bottlenecks. All students participate equally.

---

## 🛠️ Technology Stack

- **Backend**: Java 21, Spring Boot 3.3.4, Spring MVC, Spring Data JPA, Hibernate ORM
- **Database**: PostgreSQL (hosted on Supabase) via PostgreSQL JDBC Driver
- **Frontend / Templating**: Thymeleaf, Semantic HTML5, Vanilla CSS3 (Custom Glassmorphism Theme), Vanilla JavaScript (ES6+)
- **Build Tool**: Apache Maven (with Maven Wrapper `mvnw` / `mvnw.cmd` included)
- **Validation**: Jakarta Bean Validation (`@Valid`, `@NotBlank`, `@Size`) + allowed-category check in the service

---

## 🏗️ Layered Architecture

The application strictly follows a decoupled, maintainable multi-tier architecture:

```
Web Browser (UI) / REST Client
              │
              ▼
   [ Controller Layer ]
   ├── IssueWebController      (Thymeleaf Web UI routes)
   └── IssueRestController     (JSON REST API endpoints)
              │
              ▼
    [ Service Layer ]
   ├── IssueService            (Business logic interfaces)
   ├── IssueServiceImpl        (Voting, priority calculation, 5-confirmation resolve rule)
   └── IssueRules              (All thresholds & categories in one place)
              │
              ▼
   [ Repository Layer ]
   ├── IssueRepository         (CRUD on 'issues' table)
   ├── IssueVoteRepository     (Tracking 'issue_votes' & duplicate prevention)
   ├── ResolutionConfirmationRepository (Tracking 'resolution_confirmations')
   └── IssueSummaryRepository  (Querying 'issue_summary' PostgreSQL view)
              │
              ▼
   [ PostgreSQL Database on Supabase ]
   ├── Table: issues
   ├── Table: issue_votes
   ├── Table: resolution_confirmations
   └── View:  issue_summary
```

---

## 🔐 Supabase Database Configuration

Spring Boot reads PostgreSQL connection credentials from `.env` or environment variables. No database credentials are ever hardcoded in the source code.

> ⚠️ **Never share `.env`** (not in git, not in zip files you send to others). If it was shared, reset the database password in Supabase and update `.env`.

### 1. How to Find Connection Details in Supabase

1. Log in to your [Supabase Dashboard](https://supabase.com/dashboard).
2. Open your project and click on the **Project Settings** (gear icon) in the left sidebar.
3. Select **Database** under the Configuration menu.
4. Scroll to **Connection parameters** or **Connection string**:
   - Under **Connection string**, select the **URI** or **JDBC** tab.
   - Note the **Host**, **Port**, **Database name**, and **User**.
   - Note your database password that you created when provisioning the project.

### 2. Which Values Belong in Each Environment Variable

- `SUPABASE_DB_URL`:
  The JDBC connection string to your Supabase PostgreSQL database with SSL enabled.
  - Standard connection format:
    ```text
    jdbc:postgresql://<YOUR_HOST>:5432/postgres?sslmode=require
    ```
  - Supabase Connection Pooler format (if using pooler port 6543):
    ```text
    jdbc:postgresql://<YOUR_POOLER_HOST>:6543/postgres?sslmode=require
    ```
- `SUPABASE_DB_USERNAME`:
  Your Supabase database username (e.g. `postgres` or `postgres.<project-ref>` when using connection pooler).
- `SUPABASE_DB_PASSWORD`:
  Your Supabase database password.

### 3. Why `.env` is Ignored by Git

The `.env` file contains your real, sensitive credentials (database host, username, password). If committed to a public or private GitHub repository, these credentials could be exposed, allowing unauthorized access to your database. Therefore, `.gitignore` explicitly excludes `.env` and `.env.*`.

### 4. Why `.env.example` is Included in Git

The `.env.example` file contains safe dummy placeholders (e.g., `YOUR_HOST`, `YOUR_DATABASE_USERNAME`, `YOUR_DATABASE_PASSWORD`). It acts as a clear reference template for team members and evaluators, showing which environment variables are required to run the application without leaking any real credentials.

---

## 🚀 Running the Application

See **Quick Start** above. Configuration is resolved in this order (highest priority first):

1. Real environment variables (OS, IDE run configuration, hosting platform)
2. The `.env` file in the project folder
3. `application.properties`

So you can still set `SUPABASE_DB_URL`, `SUPABASE_DB_USERNAME` and `SUPABASE_DB_PASSWORD` as normal environment variables (e.g. on a server) and leave `.env` out completely.

Notes on Supabase connections:

- **Port 6543 (transaction pooler)** does not support server-side prepared statements. The app sets `prepareThreshold=0` automatically (`spring.datasource.hikari.data-source-properties.prepareThreshold=0`), so this works out of the box.
- The connection pool is limited to 5 connections to stay within Supabase free-tier limits.

---

## 🔍 Troubleshooting Database Connections & Environment Variables

| Issue / Error Message                                                  | Root Cause                                                           | Solution                                                                                                                                                                                         |
| :--------------------------------------------------------------------- | :------------------------------------------------------------------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `Could not resolve placeholder 'SUPABASE_DB_URL'` or `Driver claims to not accept jdbcUrl, ${SUPABASE_DB_URL}` | `.env` was not found.                                                 | Make sure the file is named exactly `.env` (not `.env.txt`), sits next to `pom.xml`, and that you start the app **from the project folder** (in an IDE: set the working directory to the project folder).            |
| `Connection to ... refused` or `Connection timed out`                  | Incorrect host, port, or network blocking port 5432 / 6543.          | Check the host in your Supabase dashboard. If your network blocks port 5432, use the Supabase Connection Pooler on port 6543. Ensure `?sslmode=require` is present at the end of the URL. Paused free-tier projects must be resumed in the dashboard. |
| `password authentication failed for user`                              | Invalid database username or password.                               | Double-check `SUPABASE_DB_PASSWORD` and `SUPABASE_DB_USERNAME` (pooler username is `postgres.<project-ref>`). Reset the password in Supabase under **Project Settings -> Database** if needed, then update `.env`.         |
| `prepared statement "S_1" already exists`                              | Transaction pooler + prepared statements.                            | Already handled by `prepareThreshold=0` in `application.properties`. If you override the datasource settings, keep that property.                                                                 |
| `relation "issues" does not exist`                                      | Schema script could not run (check the startup log for a SQL warning). | Run `src/main/resources/db/schema-postgres.sql` once in the Supabase SQL editor.                                                                                                                  |

---

## 🗄️ Database Schema Reference (Supabase)

This schema is created automatically from `src/main/resources/db/schema-postgres.sql`. You only need to run it manually if automatic creation fails.

```sql
-- 1. Issues Table
CREATE TABLE IF NOT EXISTS issues (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(100) NOT NULL,
    location VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Issue Votes Table
CREATE TABLE IF NOT EXISTS issue_votes (
    id BIGSERIAL PRIMARY KEY,
    issue_id BIGINT NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    voter_token UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_issue_vote UNIQUE (issue_id, voter_token)
);

-- 3. Resolution Confirmations Table
CREATE TABLE IF NOT EXISTS resolution_confirmations (
    id BIGSERIAL PRIMARY KEY,
    issue_id BIGINT NOT NULL REFERENCES issues(id) ON DELETE CASCADE,
    voter_token UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_issue_confirmation UNIQUE (issue_id, voter_token)
);

-- 4. Issue Summary View
CREATE OR REPLACE VIEW issue_summary AS
SELECT
    i.id,
    i.title,
    i.description,
    i.category,
    i.location,
    i.status,
    i.created_at,
    COALESCE(v.vote_count, 0)::BIGINT AS vote_count,
    CASE
        WHEN COALESCE(v.vote_count, 0) >= 20 THEN 'HIGH'
        WHEN COALESCE(v.vote_count, 0) >= 10 THEN 'MEDIUM'
        ELSE 'LOW'
    END AS priority,
    COALESCE(r.confirmation_count, 0)::BIGINT AS resolution_confirmations
FROM issues i
LEFT JOIN (
    SELECT issue_id, COUNT(*) AS vote_count
    FROM issue_votes
    GROUP BY issue_id
) v ON i.id = v.issue_id
LEFT JOIN (
    SELECT issue_id, COUNT(*) AS confirmation_count
    FROM resolution_confirmations
    GROUP BY issue_id
) r ON i.id = r.issue_id;
```

---

## 🧪 Testing Duplicate Prevention & Multi-Device Resolution

During classroom demonstrations or grading evaluations:

1. Open `http://localhost:8080`.
2. Notice the **Device ID pill** in the navbar (e.g. `Device: 8f4e2c91...`).
3. Click **Upvote** on an issue. Notice the vote count increases and the button changes to **Voted**.
4. Attempt to click it again: The system prevents duplicate votes and returns an error toast.
5. Click the **"+ New Device"** button in the navbar:
   - A fresh UUID is generated and stored in `localStorage`.
   - You can now vote again on behalf of this new student/browser identifier!
6. Click **"Confirm Resolution"** across 5 simulated devices:
   - Watch the progress bar advance `1/5 -> 2/5 -> 3/5 -> 4/5 -> 5/5`.
   - On the 5th confirmation, the issue status dynamically and automatically updates to **`RESOLVED`**!

---

### Known limitation

Votes and confirmations are tied to a random browser UUID, not to a logged-in student. Anyone can create a new UUID (the **+ New Device** button exists for demos), so one person could vote many times or resolve an issue alone. For real campus use, add student login (e.g. college email / Spring Security) and tie votes to the student account.

---

## 📡 REST API Documentation

All REST APIs return a consistent JSON response envelope:

```json
{
  "success": true,
  "message": "Operation description",
  "data": { ... },
  "timestamp": "2026-10-20T01:00:00"
}
```

### 1. Report a New Issue

- **Endpoint**: `POST /api/issues`
- **Request Body**:
  ```json
  {
    "title": "Wi-Fi outage in Library 2nd floor",
    "description": "Eduroam access point is flashing red and dropping connections.",
    "category": "Infrastructure",
    "location": "Central Library, 2nd Floor"
  }
  ```

### 2. List & Search Issues

- **Endpoint**: `GET /api/issues`
- **Query Parameters**:
  - `search` _(optional)_: Case-insensitive search across title, description, category, and location.
  - `category` _(optional)_: Filter by category (e.g. `Academic`, `Infrastructure`, `Hostel & Mess`).
  - `status` _(optional)_: Filter by `OPEN` or `RESOLVED`.
  - `sortBy` _(optional)_: `votes` (default) or `newest`.
  - `browserId` _(optional)_: Checks if the specified browser UUID has already voted/confirmed.
- `GET /api/issues/search?keyword=...` is an alias of this endpoint.

### 3. Get Single Issue

- **Endpoint**: `GET /api/issues/{id}`

### 4. Upvote an Issue

- **Endpoint**: `POST /api/issues/{id}/vote`
- **Request Body**:
  ```json
  {
    "browserId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
  }
  ```

### 5. Confirm Issue Resolution

- **Endpoint**: `POST /api/issues/{id}/confirm-resolution`
- **Request Body**:
  ```json
  {
    "browserId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
  }
  ```

### Error responses

Errors use the same envelope with `"success": false` and a proper HTTP status:
`400` invalid input (bad JSON, invalid UUID, unknown category, validation errors), `404` issue not found,
`409` duplicate vote / confirmation, `500` unexpected server error (details are only written to the server log).

### 6. Get Dashboard Statistics

- **Endpoint**: `GET /api/issues/stats`

### 7. Get Categories

- **Endpoint**: `GET /api/issues/categories`

---

## 🧪 Automated Tests

Run the test suite:

```powershell
.\mvnw.cmd test
```

All unit, integration, and MockMvc tests run against an in-memory database with PostgreSQL compatibility mode, ensuring continuous integration and test verification without exposing database secrets.
# Java
