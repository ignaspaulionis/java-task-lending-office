# Lending Desk

Employees of an office borrow equipment (laptops, monitors, phones), join a waitlist when a
device is taken and return it by a due date. The rules in the backend are intentionally
naive; your job is to fix them while keeping the API unchanged.

This repo holds two modules:
- `backend/` - Java 21 Gradle project (Spring Boot: core/persistence/api) with PostgreSQL.
- `frontend/` - an Angular app to click through the API. It is a guide only; you do not
  need to change it.

## Prerequisites
- JDK 21
- Node.js 22.22+ or 24.15+ and npm
- Docker (for PostgreSQL and the backend tests)

## Quick start
Run these from the repository root. The commands work in bash, PowerShell and cmd; on
Windows, use `gradlew.bat` instead of `./gradlew`.

1) Database: copy `.env.example` to `.env`, then start PostgreSQL:
   ```
   docker compose up -d db
   ```
2) Backend (http://localhost:8080, seeds demo employees and devices):
   ```
   cd backend
   ./gradlew :api:bootRun
   ```
3) Frontend (http://localhost:4200), in another terminal:
   ```
   cd frontend
   npm ci
   npm start
   ```

The backend reads `DB_URL`, `DB_USER` and `DB_PASSWORD` from the environment or from the
`.env` file in the repository root.

In IntelliJ IDEA, open the repository and link `backend/build.gradle` as a Gradle project
(right-click → *Link Gradle Project*) if it isn't detected automatically.

## Tests
From the `backend` folder:
- `./gradlew test` - all tests (they start their own PostgreSQL container)
- `./gradlew test --tests "*J01_*"` - tests for one task

Each task has its own test class named after the task id. The tests are the
specification: a task is done when its tests pass and all other previously passing tests
still pass. Do not change task tests to make them pass.

## API
- `GET /api/devices` -> `{ items, page, size, totalElements, totalPages }`
- `GET /api/devices/{id}`, `POST /api/devices` `{ inventoryTag, name, category }`,
  `PUT /api/devices/{id}` `{ name, status }`
- `GET /api/employees`, `POST /api/employees` `{ name, email }`,
  `PUT /api/employees/{id}` `{ name, email, active }`
- `GET /api/employees/{id}/summary` -> `{ employee, loans, waitlist }`
- `GET /api/loans?employeeId=&active=` -> loans
- `POST /api/loans` `{ deviceId, employeeId }` -> loan
- `POST /api/loans/{id}/return` `{ employeeId }` -> `{ loan, nextEmployeeId }`
- `POST /api/loans/{id}/extend` `{ employeeId }` -> loan
- `GET /api/loans/{id}/reminder` -> `{ message }`
- `POST /api/loans/{id}/cancel` `{ employeeId }`, `POST /api/loans/{id}/transfer`
  `{ fromEmployeeId, toEmployeeId }` -> loan
- `GET /api/loans/overdue` -> loans
- `POST /api/devices/{id}/waitlist` `{ employeeId }` -> `{ position, loan }`,
  `DELETE /api/devices/{id}/waitlist/{employeeId}`
- `GET /api/health` -> `{ status: "ok" }`

Errors are returned as `application/problem+json` with a `code`, e.g.
`409 { "code": "DEVICE_ALREADY_LOANED" }`.

## Assignments
- [JuniorTasks.md](JuniorTasks.md) - business rules, performance, indexes, refactoring, bug hunts and a new endpoint
- [MidTasks.md](MidTasks.md) - database design, query performance, concurrency, time and money
- [QUESTIONS.md](QUESTIONS.md) - questions about the code base to answer in writing

Reference solutions are on the `solutions` branch, one commit per task. Try a task
yourself first. The solutions add database migrations, so after switching branches reset
the local database by running `docker compose down -v` and then `docker compose up -d db`
(the tests are not affected; they always use a fresh database).

For practice in refactoring messy code under tests, also try the Java version of the
[Gilded Rose kata](https://github.com/emilybache/GildedRose-Refactoring-Kata).

## Notes
- Note in `AI_USAGE.md` whether and how you used AI.
- If you make assumptions, note them in code comments or `AI_USAGE.md`.
