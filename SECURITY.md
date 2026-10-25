# Security and release scope

This is a local-first student project, not a deployed university service. Do not use live student information until the deployment gates in [backlog](docs/backlog.md) have been completed.

Implemented protections include exact-domain validation, single-use hashed verification tokens, BCrypt passwords, enabled-account checks, server-side roles, session fixation protection, HttpOnly cookies, CSRF validation, booking ownership checks and database uniqueness.

The local profile defaults to console verification, which logs links for testing. Set `MAIL_DELIVERY=smtp` to send email locally without logging links. Console delivery is rejected outside the local profile. Never enable it publicly. Use the `prod` profile, HTTPS, secure cookies and a verified SMTP sender for deployment. Configure reverse-proxy forwarded headers only for a trusted proxy, and add edge/application abuse controls.

For a security problem, contact the repository owner privately through their established contact channel. Do not publish secrets, verification links or affected student data in a public issue.
