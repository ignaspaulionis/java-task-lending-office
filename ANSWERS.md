# Answers

Reference answers to [QUESTIONS.md](QUESTIONS.md). Yours don't have to match word for word;
what matters is the reasoning.

## Spring and JPA
1. **Open session in view** keeps the JPA `EntityManager` open for the whole HTTP request,
   including JSON serialization in the controller. The starter maps entities to DTOs in
   the controllers and touches lazy associations there (`loan.getDevice().getName()`),
   which only works because the session is still open. Teams turn it off because it hides
   lazy loading outside transactions: queries run during serialization, cause N+1 problems
   nobody sees in the service code, and hold a database connection for the whole request.
2. Spring's transaction support recognises `jakarta.transaction.Transactional` as well as
   its own `@Transactional`. The core services are registered as Spring beans in
   `ServiceConfig`, so Spring wraps them in a proxy that starts and commits a transaction
   around each annotated public method. `core` only needs the annotation's API on the
   classpath, not Spring.
3. Transactions are applied by a proxy that wraps the bean. When `transfer` calls
   `this.moveLoan(...)`, the call goes straight to the object, not through the proxy, so
   the annotation is ignored. With no surrounding transaction, each repository `save` ran
   in its own short transaction and was committed immediately, so the first change (ending
   the old loan) stayed even though the method then threw. Fix: make the public entry
   point `@Transactional` (or move the method to another bean), and validate before
   changing data.
4. **N+1**: one query loads N rows, then one more query per row loads a related row (for
   example each loan's device). In the starter: the employee summary (device per loan,
   queue per waitlist entry), the loan list `GET /api/loans` (device per loan), the
   device search (active loan per device) and the overdue list. Fixes: `join fetch` or
   `@EntityGraph` to load associations in the same query, a projection/DTO query that
   selects only the needed columns, or one batch query (`where id in (...)`).

## Database
5. A B-tree index is ordered by the value from the first character. `LIKE 'dell%'` can use
   it (it is a range), but `'%dell%'` can match anywhere, so the database must check every
   row. `ILIKE` adds case folding on top. Alternatives: a trigram index (`pg_trgm` with GIN
   or GiST), full-text search (`tsvector` and a GIN index), or, for case-insensitive
   prefix searches only, an index on `lower(name)`.
6. A partial index only contains rows matching its `WHERE` clause. A **unique** partial
   index on `loans(device_id) WHERE returned_at IS NULL` allows any number of returned
   loans per device but at most one active loan. That is exactly the business rule, and
   the database enforces it even under concurrency or for data written without the API.
   The index is also small, because most loans are eventually returned.
7. Flyway records a checksum for every applied migration and refuses to start if an
   applied file has changed. More importantly, other databases (colleagues, test, prod)
   already ran the old version; editing it would leave them in different states. Instead,
   add a new migration (`V2__…`) that changes the schema forward.
8. Both requests read "no active loan", both insert one, and the device is lent twice
   (check-then-act race). The same happens with the loan limit. Prevention: lock the rows
   you decide on (`SELECT … FOR UPDATE` / pessimistic lock on the device and employee),
   let the database enforce the rule (partial unique index, and translate the violation
   into `409`), or use optimistic locking (`@Version`) and retry or reject on conflict.

## Java
9. `Long` is an object, and `==` compares references. Autoboxing uses `Long.valueOf`,
   which returns cached instances for -128..127, so two boxed `127L` are the same object
   but two boxed `128L` are different objects. Tests start with ids 1, 2, 3, which is why
   J15 passed in tests and failed in production. Compare with `equals` (or
   `Objects.equals` when either side can be `null`), or use primitive `long`.
10. `double` is binary floating point and cannot represent most decimal fractions exactly
    (`0.1 + 0.2 != 0.3`), so money calculations drift. `BigDecimal` is exact in decimal.
    Its `equals` also compares the **scale**: `2.0` (scale 1) and `2.00` (scale 2) are not
    equal, while `compareTo` returns 0. Use `compareTo` for numeric equality, and fix the
    scale (`setScale(2)`) before comparing strings or storing.
11. With an injected `Clock`, tests can set "now" to any moment (`MutableClock`), for
    example a minute after local midnight or a date across a daylight-saving change.
    `Instant.now()` can't be controlled, so time-dependent rules would be untestable or
    flaky. In production the bean is simply `Clock.systemUTC()`.
12. Vilnius is UTC+2 in winter and UTC+3 in summer, so 23:30 UTC is 01:30 or 02:30 the
    **next** day locally. Due dates, overdue status, late fees (per started local day),
    weekend rules and report periods are all defined in Vilnius calendar days, so using
    UTC dates would be off by one day for late-evening times.

## Design and testing
13. `core` holds the business rules and depends only on interfaces it defines, so it can be
    tested and understood without Spring or a database, and the persistence technology can
    change without touching the rules. The cost is more types and mapping code (ports,
    adapters, DTOs), more files to touch for a simple feature (J18), and sometimes
    leaking concerns anyway (the entities here are JPA-annotated in `core`).
14. Examples: partial indexes (`WHERE returned_at IS NULL`) and expression indexes
    (`lower(email)`); `SELECT … FOR UPDATE` locking behaviour under concurrency; `timestamptz`
    and time-zone handling; `pg_index`/`EXPLAIN` output; Flyway migrations written in
    PostgreSQL SQL; `ILIKE` and case-sensitivity rules. H2 only emulates PostgreSQL, so
    tests could pass on H2 and fail in production, or not be possible at all.
15. The `detail` text is for humans and may change wording or be translated; clients that
    parse it break silently. A stable machine-readable `code` (`DEVICE_ALREADY_LOANED`) is
    part of the API contract: clients and tests can branch on it, the frontend can map it
    to its own messages, and RFC 9457 gives a standard shape for the rest.
