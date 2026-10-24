# Contributing

Read [requirements](docs/requirements.md), then run the project using [local setup](docs/local-setup.md).

- Keep Java code at target 17 and use the Maven wrapper.
- Create a small issue or local task record with acceptance criteria.
- Work on `feat/<topic>`, `fix/<topic>` or `docs/<topic>` branches.
- Keep booking changes inside service transactions and domain rules; retain database constraints.
- Add a new Flyway migration for schema changes after V1 has been used; do not edit an applied migration.
- Run checks relevant to the change. For booking/identity/database changes, use the full suite against a disposable PostgreSQL database.
- Record new requirements or assumptions in `docs/requirements.md`.
- Never commit credentials, student personal information, verification tokens, dependency folders or build outputs.
- Open a pull request with the provided template. Keep `main` runnable.
