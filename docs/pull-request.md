# feat: implement the Campus Slot local booking MVP

Students with verified `@iit.ac.lk` accounts can browse study rooms, computer labs and sports courts, reserve a whole facility for a fixed one-hour session, and cancel before its start. The daily allowance applies across all facility types using the Sri Lanka calendar date. Completion is derived from the scheduled end so history and the used allowance remain intact.

The Java 17/Spring Boot backend separates domain objects, rules, services, repositories and controllers. PostgreSQL migrations add uniqueness for both the facility slot and student/date. Session authentication, CSRF protection, hashed verification tokens and administrator-only management endpoints protect the API. The React interface provides student and administrator views with responsive layouts.

Includes Maven wrapper, npm lockfile, setup/API/OOP documentation, static screen wireframes, issue/PR templates and CI against PostgreSQL 17.

Validation: JDK 17 Maven `verify` passed all 17 tests (7 domain, 10 integration, zero skipped) against native PostgreSQL 17 using normal Flyway migrations and multiple database connections, including both competing-booking tests. Four frontend unit tests, four Chromium browser tests with API fixtures, the Vite production build, executable JAR packaging, and production dependency audit (zero reported vulnerabilities) passed. A separate live browser smoke test against the real API/native database passed registration, local verification, login, browsing, booking and cancellation without browser exceptions. GitHub Actions results are not yet verified because the cloud network policy blocks GitHub API access. Real SMTP and production release gates remain in `docs/backlog.md`.
