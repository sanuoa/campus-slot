# Campus Slot

A campus facility booking application for the IIT student community. Reserve a **whole study room, computer lab or sports court** for one hour, without lecturer approval.

Independent student portfolio project. Sample facilities are fictional; this is not an official IIT service.

## Start here

1. Read the [confirmed requirements](docs/requirements.md).
2. Follow the [Windows/local setup guide](docs/local-setup.md).
3. Review the [screen wireframes](docs/wireframes.html) and [Java OOP design](docs/architecture.md).
4. Use the [API guide](docs/api.md) when testing in Postman.
5. See [verification results](docs/verification.md) for what has actually been checked.

## Technology

| Layer | Language / tools |
| --- | --- |
| Frontend | JavaScript, JSX, CSS, React 19, Vite |
| Backend | Java 17, Spring Boot 4.0.9, Maven |
| Database | PostgreSQL, SQL, Flyway migrations, Spring Data JPA / Hibernate |
| Authentication | Spring Security, BCrypt passwords, server sessions, CSRF protection |
| Email | Local development links in the backend terminal; SMTP delivery outside the local profile |
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

Install a JDK 17, Node.js 22.12+ (Node 24 used in CI), and PostgreSQL. Create the database and export the variables in [local setup](docs/local-setup.md).

```powershell
# Terminal 1, from the repository root
cd backend
$env:SPRING_PROFILES_ACTIVE="local"
$env:DB_PASSWORD="your-local-database-password"
.\mvnw.cmd spring-boot:run

# Terminal 2
cd frontend
npm ci
npm run dev
```

Open **http://localhost:5173**. The Vite proxy sends `/api` requests to Spring Boot at `127.0.0.1:8080` so cookies and CSRF work on one browser origin.

The local profile adds fictional example facilities and seven days of sample sessions on the first run only. It prints verification links in the backend terminal; it does **not** prove ownership of a real mailbox. Real deployments must use SMTP and the `prod` profile.

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
