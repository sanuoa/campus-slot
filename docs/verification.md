# Verification report

Checked on 24 October 2026. This file distinguishes current cloud validation from earlier package checks and validations still needed on GitHub or before deployment.

| Check | Result |
| --- | --- |
| Java compilation with JDK 17 | Passed |
| Maven executable JAR packaging | Passed |
| Java domain tests | 7 passed |
| Native PostgreSQL 17 integration tests | 10 passed, zero skipped, using normal Flyway migrations and multiple JDBC connections |
| Frontend date/quota tests | 4 passed |
| Frontend Chromium interaction tests | 4 passed using installed Chromium; API fixtures explicitly used |
| Vite production build | Passed |
| Real HTTP browser smoke test | Passed against packaged Spring Boot API and native PostgreSQL 17: registration, verification, login, browsing, booking and cancellation; no browser exceptions |
| Desktop and mobile inspection | Earlier package inspection at 1440px and 390px; current browser suite also checks no horizontal overflow at 390px |
| Frontend production dependency audit | 0 reported vulnerabilities at verification time; not a guarantee of overall security |
| Native PostgreSQL 17 concurrency tests | Passed: same-slot competition and same-student/different-room competition each produce exactly one booking |
| GitHub publication | Feature branch pushed successfully using HTTPS Git authentication; Actions results unverified because API requests are blocked by environment network policy |
| SMTP verification against real IIT inboxes | Pending before deployment |
| Windows installation on the owner's PC | Instructions supplied; pending owner run |

## Current cloud validation

The cloud run used JDK 17, Node 24, and a Docker PostgreSQL 17 service. `RUN_DB_TESTS=true` and a separate disposable `campus_slot_test` database enabled all 17 backend tests (7 domain, 10 integration; zero failures/errors/skips). The application database was separate. Maven `verify`, frontend unit tests, Vite build and the production dependency audit passed; the audit reported zero vulnerabilities at this run. TLS verification stayed enabled; Maven used the platform proxy and system Java trust store.

The Playwright browser download was denied by network policy. All four existing browser tests passed using a temporary configuration selecting `/usr/bin/chromium`; tracked test configuration was unchanged. A separate live browser smoke test used the actual Vite proxy, API, sessions, CSRF tokens, local verification sender and native database. No real student data or real email delivery was used.

## Earlier package harness and its limits

In the earlier package environment, the runtime could not start an ordinary PostgreSQL server as a non-root user. A temporary **PGlite PostgreSQL engine**, exposed over its socket adapter, was used for integration checks. The repository itself still uses native PostgreSQL and does not depend on PGlite.

The V1 migration SQL was applied directly to that engine before starting tests; Flyway was disabled only in this temporary harness. Hibernate validated the resulting schema. The harness used a single JDBC connection because of PGlite's connection model. It verified registration, single-use verification, role restrictions, CSRF, booking ownership, cancellation, quota and database unique indexes. Competing request tests passed, but serialization through one connection means these results do **not** validate native PostgreSQL lock behaviour under multiple simultaneous database connections.

The checked-in CI suite uses a real PostgreSQL 17 service with the normal Flyway startup and normal connection pool. Native PostgreSQL concurrency has now also passed locally in the cloud run above. Inspect the actual GitHub CI result before merging.

## Browser checks

Four checked-in Playwright tests use API fixtures to test registration payloads, confirmation interactions, completed-session quota UI and administrator/mobile behaviour. Separate temporary live browser validation used the actual packaged Spring Boot application and its session/CSRF implementation. It exercised registration, local verification, login, browsing, booking, cancellation, logout and administrator facility creation with no browser exceptions.

Test accounts were fictional; local development verification did not send messages to real inboxes. Visual QA screenshots were internal checks and are not included in the source package.

## Release status

Local MVP candidate. Public deployment remains gated by GitHub CI, real mail delivery, account recovery/abuse controls, actual campus data and the production checks in [backlog](backlog.md).
