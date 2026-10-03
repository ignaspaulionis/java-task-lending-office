# Questions

Answer in a few sentences each, as you would explain it to an interviewer. Look at the code
before answering. Reference answers are in `ANSWERS.md` on the `solutions` branch.

## Spring and JPA
1. `spring.jpa.open-in-view` is `true` in `application.yaml`. What does it do, why does
   the starter code depend on it, and why do many teams turn it off?
2. The services in `core` are annotated with `@jakarta.transaction.Transactional`, but
   `core` has no Spring dependency. How do these annotations still start transactions?
3. In `LoanTransferService` (J16), `moveLoan` is annotated `@Transactional`. Why did that
   not help when `transfer` called it?
4. What is the N+1 query problem? Name two places in this project where it happens in the
   starter code and two ways to fix it.

## Database
5. Why does PostgreSQL not use a normal B-tree index for `name ILIKE '%dell%'`? What could
   you use instead?
6. What is a partial index (`... WHERE returned_at IS NULL`), and why is it a good fit for
   "one active loan per device"?
7. Why must you never edit `V1__init.sql` once it has been applied, and what do you do
   instead?
8. Two requests try to borrow the same device at the same moment. What can go wrong in the
   starter code, and what are two ways to prevent it?

## Java
9. Why does `Long a = 127L, b = 127L; a == b` give `true`, but the same with `128L` give
   `false`? How do you compare ids correctly?
10. Why is money (late fees) a `BigDecimal` and not a `double`? Why can
    `new BigDecimal("2.0").equals(new BigDecimal("2.00"))` surprise you?
11. Why do the services take a `java.time.Clock` instead of calling `Instant.now()`?
12. A loan is due at 23:30 UTC. Which calendar day is that in Vilnius, and why does it
    matter for this project?

## Design and testing
13. The code is split into `core`, `persistence` and `api`, and `core` only knows
    repository *interfaces* (ports). What does that buy you, and what does it cost?
14. The tests run against a real PostgreSQL in Docker (Testcontainers) instead of H2. Name
    two things that would behave differently, or not be testable, with H2.
15. Errors are returned as `application/problem+json` with a stable `code`. Why is a
    stable code better than letting clients read the `detail` text?
