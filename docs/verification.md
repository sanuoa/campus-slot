# Testing

## Automated checks

From `frontend/`:

```shell
npm ci
npm test
npm run build
npx playwright install chromium
npm run test:e2e
```

Unit tests cover date handling and the daily allowance. Browser tests cover booking confirmation, completed-session quota, registration, administrator/mobile behaviour, console-delivery messaging and verification resends. Browser tests use explicit API fixtures; they do not prove backend or inbox delivery.

From `backend/`, use the Maven wrapper (`mvnw.cmd` on Windows):

```shell
./mvnw test
```

This runs domain and mail-delivery tests. To include database integration tests, create a **disposable PostgreSQL database**, export `RUN_DB_TESTS=true`, `DB_URL`, `DB_USERNAME` and `DB_PASSWORD`, then run `./mvnw verify`. Integration tests truncate application tables. Never point them at an application or production database. See [local setup](local-setup.md) for Windows commands.

Integration tests exercise registration, single-use verification, credential-protected resends, resend cooldown, token replacement, mail-failure rollback, permissions, CSRF, quota, cancellation, uniqueness and competing bookings against native PostgreSQL. GitHub Actions provides a separate PostgreSQL service.

## Manual email checks

Configure [Gmail SMTP](local-setup.md#send-verification-emails-with-gmail), register an IIT account, and verify the message arrives in its actual inbox. Open the link, log in, book and cancel a future session. Check resend for an unverified account after the cooldown, confirm the older link stops working, and confirm a verified account receives no new link. Never publish credentials, real student records or verification links in test reports.

Automated mail-client tests and local SMTP capture establish integration behaviour; real Gmail authentication, institutional filtering and inbox delivery require the configured sender and recipient mailboxes. Production checks remain in the [roadmap](backlog.md).
