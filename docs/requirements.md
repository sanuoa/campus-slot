# Campus Slot requirements

Facility booking requirements. All dates and times shown in the interface use `Asia/Colombo`.

## Confirmed scope

| Topic | Requirement |
| --- | --- |
| Facilities | Study rooms, computer labs, sports courts; reserve the entire facility, never individual seats |
| Approval | Immediate confirmation; no lecturer approval |
| Duration | Exactly one hour from the scheduled start, not from the time the booking is created |
| Daily limit | A student may have one non-cancelled one-hour session per Sri Lanka date across all types |
| End of session | Automatically release use of the facility after the scheduled hour and retain history |
| Identity | Exactly `@iit.ac.lk` accounts; real deployments verify mailbox ownership |
| Technology | Java OOP and Spring Boot; React frontend; PostgreSQL database |
| Development | Test locally before considering free or paid deployment |

## Initial implementation decisions

The application uses these defaults:

- One campus; student and administrator roles.
- Slots start at whole hours in Sri Lanka time and may be published up to 30 days ahead.
- Cancel only before the slot starts. A cancellation restores the daily allowance; a completed or ongoing session does not.
- Sample opening hours are 08:00–18:00, published for seven days on first local startup. Administrators choose actual hours per facility/date.
- Capacity is descriptive; no attendee roster, group invitations or individual seat counts.
- An email domain plus mailbox verification limits access to that domain, but cannot distinguish a student from staff using the same domain. Stronger student eligibility needs an official roster or institutional identity provider and is a deployment decision.
- Completion is a time-derived API status. Persisted `CONFIRMED` bookings continue to consume the date's allowance. No fragile background timer is required for correctness.
- No payment, lecturer role or external timetable connection in v0.1.

## User acceptance scenarios

1. A new student registers with an IIT email, verifies it and logs in.
2. A student selects a date, browses facility types, chooses an available session and receives confirmation.
3. The entire facility is unavailable to other students for that hour.
4. Another booking by that student on the same date is rejected, including a different facility type.
5. Two requests arriving at once cannot obtain the same facility slot or bypass the daily limit.
6. Cancellation before the start makes both the slot and that student's allowance available again.
7. After the scheduled end, the booking is shown as completed and remains in history. The next hour is a separate slot.
8. An administrator can publish, block/unblock and manage sessions without exposing student identities in the student availability API.

## Verification email

Use Gmail SMTP or another configured SMTP service for real email, including local testing. Console delivery remains available only in the local profile and is clearly identified in the interface. Unverified users can resend a link using their account credentials, at most once per minute; newer links invalidate older ones.
