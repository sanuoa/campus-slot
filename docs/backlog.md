# Project task list

Local task records are provided because GitHub rejected branch and issue creation with HTTP 403, `Resource not accessible by integration`. No GitHub issues or pull request have been claimed as created. Convert these records into issues after write access is available.

## MVP implementation

| ID | Task | Acceptance |
| --- | --- | --- |
| CS-01 | Identity and authorization | Exact email domain; verification; session login; CSRF; administrator permissions |
| CS-02 | Booking domain and database | Whole-facility one-hour slots; daily quota; completion; cancellation; partial unique indexes |
| CS-03 | Student interface | Registration/login, facility filters, date selection, slot confirmation, booking history |
| CS-04 | Administrator interface | Add/edit facilities, publish slots, block/unblock, view/cancel future bookings |
| CS-05 | Delivery foundation | Maven wrapper, npm lockfile, tests, CI workflow, setup/API/OOP docs and wireframes |

Implementation and actual verification status are recorded separately in [verification.md](verification.md).

## Before public deployment

- **CS-06 — Publish repository work.** Grant the GitHub integration write access for contents, issues and pull requests; push `feat/campus-slot-mvp`, open a PR and inspect native PostgreSQL CI. Set `main` checks through repository settings when available. Branch protection is not configured by this package.
- **CS-07 — Validate actual campus rules/data.** Replace fictional facility names and hours, confirm whether exactly `@iit.ac.lk` covers student accounts, and define how staff sharing the domain are excluded if necessary. Review the cancellation-refunds-quota and 30-day defaults with the owner.
- **CS-08 — Account recovery and abuse controls.** Implement verification resend, password reset, registration/login/resend rate limits, and cleanup/retention for unverified accounts and tokens. Never expose tokens in deployed logs.
- **CS-09 — Production delivery.** Configure and test SMTP with a verified sender; test domain ownership verification using real inboxes and TLS. Review deliverability separately from address suffix validation.
- **CS-10 — Deploy and validate.** Compare current free/paid hosts at deployment time; select a Java-capable host plus managed PostgreSQL, HTTPS, durable storage, backups and an `/api` same-origin proxy. Test secure cookies, logout, authorization, concurrent requests and restore procedures. The owner has authorized local testing first; no hosting account or paid resource has been created.

## Working method

Use an issue with a small acceptance checklist, then a `feat/…`, `fix/…` or `docs/…` branch. Commit focused changes using conventional prefixes. Open a PR explaining the resulting behaviour and checks. Merge when relevant checks pass, then update this task list. Do not fabricate review approvals, past activity or successful CI runs.

Future maintenance is done when work is requested or an explicitly configured automation runs; this repository setup does not create a background maintenance service.
