# Verification report

Checked on 24 October 2026. This file distinguishes implemented checks from validations still needed on the owner's machine or GitHub.

| Check | Result |
| --- | --- |
| Java compilation with JDK 17 | Passed |
| Maven executable JAR packaging | Passed |
| Java domain tests | 7 passed |
| Backend integration test harness | 10 passed, with the limitations below |
| Frontend date/quota tests | 4 passed |
| Frontend Chromium interaction tests | 4 passed; API fixtures explicitly used |
| Vite production build | Passed |
| Real HTTP browser smoke test | Passed against the actual Spring Boot API and test database engine |
| Desktop and mobile inspection | Checked at 1440px and 390px; no horizontal overflow at 390px |
| Frontend production dependency audit | 0 reported vulnerabilities at verification time; not a guarantee of overall security |
| Native PostgreSQL 17 integration and concurrency tests | Configured in CI; not executed in this environment |
| GitHub Actions run | Not executed: GitHub rejected repository writes |
| SMTP verification against real IIT inboxes | Pending before deployment |
| Windows installation on the owner's PC | Instructions supplied; pending owner run |

## Backend harness and its limits

The runtime could not start an ordinary PostgreSQL server as a non-root user. A temporary **PGlite PostgreSQL engine**, exposed over its socket adapter, was used for integration checks. The repository itself still uses native PostgreSQL and does not depend on PGlite.

The V1 migration SQL was applied directly to that engine before starting tests; Flyway was disabled only in this temporary harness. Hibernate validated the resulting schema. The harness used a single JDBC connection because of PGlite's connection model. It verified registration, single-use verification, role restrictions, CSRF, booking ownership, cancellation, quota and database unique indexes. Competing request tests passed, but serialization through one connection means these results do **not** validate native PostgreSQL lock behaviour under multiple simultaneous database connections.

The checked-in CI suite uses a real PostgreSQL 17 service with the normal Flyway startup and normal connection pool. It must pass before merging or claiming native PostgreSQL concurrency has been verified.

## Browser checks

Four checked-in Playwright tests use API fixtures to test registration payloads, confirmation interactions, completed-session quota UI and administrator/mobile behaviour. Separate temporary live browser validation used the actual packaged Spring Boot application and its session/CSRF implementation. It exercised registration, local verification, login, browsing, booking, cancellation, logout and administrator facility creation with no browser exceptions.

Test accounts were fictional; local development verification did not send messages to real inboxes. Visual QA screenshots were internal checks and are not included in the source package.

## Release status

Local MVP candidate. Public deployment remains gated by native PostgreSQL validation, GitHub CI, real mail delivery, account recovery/abuse controls, actual campus data and the production checks in [backlog](backlog.md).
