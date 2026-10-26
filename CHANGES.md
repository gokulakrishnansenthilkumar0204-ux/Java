# Changes (fix release)

## Startup / configuration
- `.env` is now loaded automatically (`spring.config.import=optional:file:.env[.properties]`). The old error
  `Driver claims to not accept jdbcUrl, ${SUPABASE_DB_URL}` is gone; no PowerShell/bash loading step needed.
- Tables, indexes and the `issue_summary` view are created automatically from `db/schema-postgres.sql`.
- `prepareThreshold=0` for the Supabase transaction pooler (port 6543) – fixes "prepared statement already exists".
- Small Hikari pool (5) for Supabase free tier; SQL logging and the redundant dialect setting removed.

## Backend
- Correct HTTP status codes for the REST API: 400 (bad JSON, invalid UUID, non-numeric id, validation, unknown
  category), 404, 409 (duplicates, incl. database unique-constraint race), 415, 500 without leaking internals.
- Error handler is scoped to the REST controller, so `/favicon.ico` etc. give 404 instead of 500.
- Row locking (`SELECT ... FOR UPDATE`) on vote / confirm – simultaneous clicks can't create duplicates or break the 5-confirmation rule.
- Issue list: 2 queries for "has voted / has confirmed" instead of 2–3 queries per issue.
- All thresholds and categories in one class (`IssueRules`); API returns `confirmationsRequired`.
- Category must be one of the predefined categories; description limited to 5000 characters.
- No-JavaScript form fallback now validates input and shows success / error messages on the page.
- Timestamps in API responses include the time zone; unused code removed.

## Frontend
- No more flicker: the list refreshes in place after votes / confirmations / new issues.
- Out-of-date search responses are ignored (fast typing no longer shows wrong results).
- Buttons are disabled while a request runs (no double votes from double clicks).
- Safe handling when localStorage is blocked and when the server returns a non-JSON error.

## Tests
- New tests: error status codes, confirm-after-resolved, category validation, per-browser "voted" flags in the list.
