# Junior tasks

Backend only. Each task takes about 15–25 minutes and has its own test class in
`backend/api/src/test/java/com/lendingdesk/junior` (`J01_…Test` and so on).

Rules already defined in the code (`LendingRules`):
- An employee may have at most **3** active loans.
- A loan lasts **14** days.
- Only `AVAILABLE` devices can be borrowed.

## J01 - No double loans
At the moment, any device can be borrowed, even one that is already on loan.
- A device that is already on loan cannot be borrowed again, not even by the same
  employee: `409 DEVICE_ALREADY_LOANED`.
- Once the device is returned, it can be borrowed again.
- A device in `MAINTENANCE` or `RETIRED` cannot be borrowed: `409 DEVICE_NOT_AVAILABLE`.
- An inactive employee cannot borrow: `409 EMPLOYEE_INACTIVE`.

## J02 - Only the borrower returns
At the moment, anyone can return any loan, and a loan can be returned twice.
- Only the employee who borrowed the device can return it: `409 NOT_LOAN_OWNER`.
- Returning an already returned loan fails and keeps the original return time:
  `409 LOAN_ALREADY_RETURNED`.
- Returning an unknown loan fails: `404 LOAN_NOT_FOUND`.

## J03 - Waitlist
At the moment, joining the waitlist always succeeds.
- An employee can join the waitlist for a loaned device. The response shows their
  position (1, 2, …).
- The same employee cannot join the same waitlist twice: `409 ALREADY_WAITLISTED`.
- The employee who currently holds the device cannot join its waitlist:
  `409 ALREADY_WAITLISTED`.
- Joining the waitlist for a free device loans it to that employee immediately. The
  response then contains the `loan` instead of a `position`.
- Leaving a waitlist you are not on fails: `409 NOT_WAITLISTED`. Leaving keeps everyone
  else in order.

## J04 - Hand the device to the next in line
At the moment, a return only reports who is first in the queue. The device is not
loaned to them and the queue never shrinks.
- On return, the device is loaned to the first eligible employee in the queue, and that
  employee is removed from the queue. The response says who got it (`nextEmployeeId`).
- Inactive employees and employees at their loan limit are skipped and removed from the
  queue.
- If nobody is eligible, the device becomes free and `nextEmployeeId` is `null`.
- A device that is not `AVAILABLE` (for example in `MAINTENANCE`) is not handed over; the
  queue waits.
- While a queue exists, only the first person in it may borrow the device:
  `409 NOT_FIRST_IN_QUEUE`.

## J05 - Loan limit, clean and cheap
At the moment, the limit check loads every loan in the database and counts them in Java,
and it allows one loan too many.
- A 4th active loan is rejected: `409 LOAN_LIMIT_REACHED`.
- Returned loans do not count, and one employee's loans do not affect another's.
- The check counts active loans in the database; borrowing does not load any loans into
  memory.

## J06 - Overdue list in the database
At the moment, `GET /api/loans/overdue` loads all loans and filters them in Java.
- Only active loans whose due time has passed are returned, most overdue first.
- Returned loans are never listed.
- The endpoint runs a single SQL query and loads only the overdue loans, no matter how
  many other loans exist.
