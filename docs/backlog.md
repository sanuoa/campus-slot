# Roadmap

## Before public deployment

- Confirm actual campus facility data, opening hours and student eligibility rules. The fixtures are fictional; an email suffix alone cannot distinguish students from staff.
- Add password reset, application/edge registration and login rate limits, and retention policies for unverified accounts and tokens. Resend already requires account credentials and has a one-minute cooldown; broader abuse controls remain necessary.
- Validate Gmail or another SMTP sender against real IIT inboxes, including spam filtering and delivery failures.
- Configure HTTPS, secure session cookies, a same-origin `/api` proxy and a publicly reachable frontend URL. Keep console verification disabled.
- Configure managed PostgreSQL backups and test restoration. Validate authentication, authorization and concurrent booking requests in the deployed environment.
- Require the backend and frontend CI jobs before merging changes.
