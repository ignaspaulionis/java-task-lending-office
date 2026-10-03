# Mid tasks

Backend only. Each task takes about 30–60 minutes and has its own test class in
`backend/api/src/test/java/com/lendingdesk/mid` (`M01_…Test` and so on). Several tests
assume the junior rules work, so solve [JuniorTasks.md](JuniorTasks.md) first or start
from the `solutions` branch.

## M01 - Indexes and database rules
The schema (`V1__init.sql`) has primary keys, foreign keys and a unique inventory tag.
Add a new Flyway migration; do not edit `V1`.
- The database itself guarantees that a device has at most one active loan
  (a partial unique index).
- Looking up an employee's active loans uses an index.
- An employee can be on a device's waitlist only once, and reading a queue in order
  (`device_id, created_at`) uses an index.
- Be ready to explain why a search like `name ILIKE '%dell%'` gets no ordinary index.

## M02 - Employee summary without N+1
At the moment, `GET /api/employees/{id}/summary` runs extra queries for every loan and
every waitlist entry.
- The response is unchanged: active loans with device names, and waitlist positions, in
  the order they were created.
- The number of SQL queries is the same whether the employee has 1 loan or 3 loans and
  5 waitlist entries, and at most 3.

## M03 - Device search and paging in SQL
At the moment, `GET /api/devices` loads every device, then filters and pages them in Java
with one extra query per device, and it ignores sorting.
- Filters: `q` (case-insensitive part of name or tag, `%` and `_` match literally),
  `category`, `available`. They work alone and together.
- Paging with `page` and `size`; `size` is capped at 50; correct `totalElements` and
  `totalPages`.
- Sorting by `name` or `inventoryTag`, `asc` or `desc`; ties are ordered by id. An unknown
  sort field or direction returns `400 VALIDATION_FAILED`.
- At most 3 SQL queries, and only the requested page of devices is loaded.

## M04 - Concurrent borrows
At the moment, simultaneous requests can borrow the same device, or push an employee over
the limit.
- 10 parallel borrows of one device: exactly one succeeds, the rest get
  `409 DEVICE_ALREADY_LOANED` (not `500`).
- 6 parallel borrows of different devices by one employee: exactly 3 succeed, the rest get
  `409 LOAN_LIMIT_REACHED`.

## M05 - Late fees
At the moment, loans have no fee, and "overdue" compares exact instants in UTC.
- The late fee is €0.50 per started overdue day, at most €20.00 per loan. Use
  `BigDecimal`. Every active loan shows its fee as a string with two decimals; a loan that
  is not overdue shows `"0.00"`.
- Days are calendar days in Europe/Vilnius: a loan is overdue from the first local
  midnight after its due time. A loan due at 23:30 UTC is due on the next local day.
- `GET /api/loans/overdue` uses the same rule.
- Tests set the current time through the injected `Clock`.

## M06 - CSV device import
Implement `POST /api/devices/import` (multipart field `file`, UTF-8).
- The first line must be exactly `inventoryTag,name,category`. Fields never contain
  commas. Blank lines and Windows line endings are allowed.
- Valid rows are imported. Invalid rows are skipped and reported with their line number
  (the header is line 1) and one of these reasons:
  - `MISSING_FIELD`: a field is empty, or the line doesn't have exactly three fields.
  - `INVALID_TAG`: the tag doesn't start with `NTL-`.
  - `FIELD_TOO_LONG`: longer than the column allows (tag 50, name 100, category 50).
  - `DUPLICATE_IN_FILE`: the tag already appeared on an earlier line.
  - `TAG_EXISTS`: the tag is already in the database.
- Response `200 { imported, errors: [{ line, reason }] }`.
- A wrong header or an empty file returns `400 VALIDATION_FAILED` and imports nothing.

## M07 - Top devices report
Implement `GET /api/reports/top-devices?from=&to=` (local dates in Vilnius, `to`
exclusive).
- Returns the 5 devices borrowed most often in the period:
  `[{ deviceId, deviceName, loanCount }]`. Ties are ordered by name.
- `from` after `to` returns `400 VALIDATION_FAILED`. A period with no loans returns an
  empty list.
- Runs a single SQL query.
