# feat: implement the Campus Slot local booking MVP

Students with verified `@iit.ac.lk` accounts can browse study rooms, computer labs and sports courts, reserve a whole facility for a fixed one-hour session, and cancel before its start. The daily allowance applies across all facility types using the Sri Lanka calendar date. Completion is derived from the scheduled end so history and the used allowance remain intact.

The Java 17/Spring Boot backend separates domain objects, rules, services, repositories and controllers. PostgreSQL migrations add uniqueness for both the facility slot and student/date. Session authentication, CSRF protection, hashed verification tokens and administrator-only management endpoints protect the API. The React interface provides student and administrator views with responsive layouts.

Includes Maven wrapper, npm lockfile, setup/API/OOP documentation, static screen wireframes, issue/PR templates and CI against PostgreSQL 17.

Validation: 7 domain tests and 10 integration tests passed in the temporary PGlite harness; 4 frontend unit tests and 4 browser tests passed; executable JAR and frontend production builds passed. A live HTTP browser smoke flow also passed. Native PostgreSQL/Flyway/concurrency CI and real SMTP delivery remain required, as described in `docs/verification.md`.
