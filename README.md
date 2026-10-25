# Campus Slot

A campus facility booking application for the IIT student community. Reserve a **whole study room, computer lab or sports court** for one hour, without lecturer approval.

Independent student portfolio project. Sample facilities are fictional; this is not an official IIT service.

## Start here

1. Read the [confirmed requirements](docs/requirements.md).
2. Follow the [Windows/local setup guide](docs/local-setup.md).
3. Review the [screen wireframes](docs/wireframes.html) and [Java OOP design](docs/architecture.md).
4. Use the [API guide](docs/api.md) when testing in Postman.
5. See [testing](docs/verification.md) for the test suites and their scope.

## Technology

| Layer | Language / tools |
| --- | --- |
| Frontend | JavaScript, JSX, CSS, React 19, Vite |
| Backend | Java 17, Spring Boot 4.0.9, Maven |
| Database | PostgreSQL, SQL, Flyway migrations, Spring Data JPA / Hibernate |
| Authentication | Spring Security, BCrypt passwords, server sessions, CSRF protection |
| Email | Terminal links for development, or authenticated SMTP (including Gmail) for real delivery |
| Tests | JUnit, MockMvc, PostgreSQL integration tests, Vitest |
| Repository | GitHub feature branches, pull requests, issue templates, GitHub Actions |

The frontend calls **our own Spring Boot REST API**. SMTP is the only external service required for real email verification. Node.js runs frontend development/build tools; application business logic runs in Java.

## Booking rules

- Sign up using exactly `@iit.ac.lk`; verify the address before login.
- Student and administrator roles. Students cannot promote themselves.
- Whole-facility bookings in fixed one-hour sessions, starting on the hour.
- One non-cancelled session per student per **Sri Lanka calendar date**, across every facility type.
- Confirm immediately if available. PostgreSQL constraints and transaction locks prevent double booking.
- At the scheduled end, the API shows the session as **completed** automatically; its history is retained and its daily allowance stays used.
- Cancel before the session starts to release the space and restore that day's allowance.
- An administrator creates facilities, publishes sessions and blocks availability.

## Quick run

Install JDK 17, Node.js 22.12+ (Node 24 used in CI), and PostgreSQL. Complete the one-time database creation in [local setup](docs/local-setup.md) first; keep the existing database for subsequent runs.

For an everyday local run, open **two Windows Command Prompt (CMD) windows in the repository root**. These commands use CMD syntax; the [local setup guide](docs/local-setup.md) uses PowerShell.

**CMD 1 — backend:**

```bat
cd backend
set "SPRING_PROFILES_ACTIVE=local"
set "MAIL_DELIVERY=console"
set "DB_URL=jdbc:postgresql://localhost:5432/campus_slot"
set "DB_USERNAME=campus_slot"
set /p "DB_PASSWORD=Enter campus_slot database password: "
mvnw.cmd spring-boot:run
```

Enter the password for the PostgreSQL `campus_slot` account, not your Gmail or Campus Slot login password. Wait for `Started CampusSlotApplication` and keep this window open. Set these variables again whenever you open a new backend terminal.

**CMD 2 — frontend:**

```bat
cd frontend
npm run dev
```

Run `npm ci` in `frontend` before starting it on first setup or after dependency changes. Keep this window open too.

**Database:** PostgreSQL normally starts automatically with Windows. If it is stopped, start its service through Windows Services. For a standard PostgreSQL 17 installation, an administrator CMD can run:

```bat
net start postgresql-x64-17
```

The service name depends on the installed version; check Windows Services if that name is not found. Do not recreate the database for each run.

Open **http://localhost:5173**. The Vite proxy sends `/api` requests to Spring Boot at `127.0.0.1:8080` so cookies and CSRF work on one browser origin.

With `MAIL_DELIVERY=console`, **no email is sent and Gmail is not required**. Register, or enter an existing unverified account's email and password and click **Resend verification link**. Allow one minute between links, then copy the private verification URL from the backend terminal, open it, and click **Verify my email**. Log in to reach the student dashboard. Already verified accounts can log in directly.

The local profile adds fictional example facilities and seven days of sample sessions on the first empty-database startup only. For real email delivery, follow the [Gmail SMTP setup](docs/local-setup.md#send-verification-emails-with-gmail) and use `MAIL_DELIVERY=smtp`. Public deployments require SMTP and the `prod` profile; console verification is only for local development.

Press **Ctrl+C** in each application window to stop it. PostgreSQL retains the records between runs.

## Test

```powershell
cd frontend
npm test
npm run build

cd ../backend
.\mvnw.cmd test
```

The command above runs domain tests and explicitly skips database integration tests. To run the full suite, use a **disposable test database**, set `RUN_DB_TESTS=true` and `DB_URL` to that database, then run `mvnw verify`. Integration tests truncate application tables. See the exact commands in the setup guide. GitHub Actions is configured to run both suites against a separate PostgreSQL service.

## Repository layout

```text
backend/       Java domain objects, controllers, services, repositories, migrations and tests
frontend/      React screens, API client, CSS and date/quota tests
docs/          Requirements, OOP architecture, wireframes, APIs, setup and deployment gates
.github/       CI workflow, pull request template and issue template
```

## Scope and completion

Version 0.1 targets a locally runnable MVP. It is finished when verified students can book/cancel, admins can manage availability, daily limits and races are tested, and setup instructions work. Real campus data, delivery configuration, production abuse controls, hosting and deployment validation are separate items in the [tracked backlog](docs/backlog.md). There is no payment or external campus timetable integration in this version.
