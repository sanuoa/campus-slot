# REST API

Base path: `/api`. Request and response bodies are JSON except login, which uses URL-encoded form data. Authentication uses a session cookie, not a JWT. Timestamps are UTC ISO instants; the interface displays Sri Lanka time.

Before **every mutation**, GET `/api/auth/csrf`, retain the returned session cookie, and send the returned `token` in the header identified by `headerName`. Postman keeps cookies in its cookie jar. Example response:

```json
{"headerName":"X-CSRF-TOKEN","parameterName":"_csrf","token":"value-from-server"}
```

## Identity

| Method and path | Body / response | Access |
| --- | --- | --- |
| `GET /auth/csrf` | CSRF token and header name | Public |
| `POST /auth/register` | `{ "name": "Student", "email": "student@iit.ac.lk", "password": "a long unique password" }`; 201 | Public, CSRF required |
| `POST /auth/verify` | `{ "token": "token-from-link" }`; 204 | Public, CSRF required |
| `POST /auth/login` | Form fields `username` (email), `password`; 204 plus session cookie | Public, CSRF required |
| `GET /auth/me` | `{ "id": 1, "name": "Student", "email": "student@iit.ac.lk", "role": "STUDENT" }` | Signed in |
| `POST /auth/logout` | No body; 204 | Signed in, CSRF required |

Passwords need at least 12 characters and at most 72 UTF-8 bytes. Names are at most 80 characters. Only exactly `@iit.ac.lk` is accepted, case-normalized. Registration ignores any client-supplied role and creates `STUDENT` only. Verification tokens expire after 24 hours and are single-use. Expired-link recovery is a tracked follow-up; local development can reset its disposable account database rather than reusing an expired account.

## Student facilities and bookings

| Method and path | Body / response |
| --- | --- |
| `GET /facilities` | Active facilities; admins additionally see inactive facilities |
| `GET /facilities/{id}/slots?date=2026-10-25` | Slots with `id`, `startsAt`, `endsAt`, `state` |
| `POST /bookings` | `{ "slotId": 1 }`; 201 booking view; verified students only |
| `GET /bookings/mine` | Current user's booking history |
| `POST /bookings/{id}/cancel` | Cancel own booking before start; cancelled booking view |

Slot states: `AVAILABLE`, `BOOKED`, `BLOCKED`, `PAST`. Booking statuses: `CONFIRMED`, `CANCELLED`, `COMPLETED`. Availability never exposes the identity of another student. Booking views include facility ID/name/type, start/end, Sri Lanka date, status and the booking owner's details. Students can retrieve only their own booking views.

## Administration

| Method and path | Body / response |
| --- | --- |
| `POST /admin/facilities` | `{ "name": "Room A", "type": "STUDY_ROOM", "location": "Level 1", "capacity": 6 }`; 201 |
| `PUT /admin/facilities/{id}` | Same fields plus `active`; updated facility |
| `POST /admin/facilities/{id}/slots` | `{ "date": "2026-10-25", "openingHour": 8, "closingHour": 18 }`; `{ "created": 10 }` |
| `PATCH /admin/slots/{id}` | `{ "blocked": true }`; 204 |
| `GET /admin/bookings?date=2026-10-25` | All bookings on that date, with student details |
| `POST /admin/bookings/{id}/cancel` | Cancel a future booking; updated view |

Facility types: `STUDY_ROOM`, `COMPUTER_LAB`, `SPORTS_COURT`. Generation keeps existing slots and skips starts that are already past. Opening hour is 0–23, closing hour 1–24, with closing after opening. Existing bookings must be cancelled before blocking their slot or deactivating their facility. A session already in progress cannot be cancelled.

## Errors

Errors use `{ "message": "Readable explanation" }`. Typical responses: 400 invalid fields/date/domain, 401 unauthenticated or invalid login, 403 insufficient role or CSRF failure, 404 missing or another student's booking, 409 quota/availability conflict, 503 mail delivery unavailable. Reload availability after a 409. Tokens and database exception details are never returned.
