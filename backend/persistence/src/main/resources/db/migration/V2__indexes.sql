-- A device can have at most one active loan; returned loans are not restricted.
CREATE UNIQUE INDEX ux_loans_active_device ON loans (device_id) WHERE returned_at IS NULL;

-- Counting and listing an employee's active loans.
CREATE INDEX ix_loans_active_employee ON loans (employee_id) WHERE returned_at IS NULL;

-- An employee waits for a device at most once.
CREATE UNIQUE INDEX ux_waitlist_device_employee ON waitlist_entries (device_id, employee_id);

-- Reading a device's queue in order.
CREATE INDEX ix_waitlist_device_order ON waitlist_entries (device_id, created_at, id);

-- Finding the waitlists an employee is on.
CREATE INDEX ix_waitlist_employee ON waitlist_entries (employee_id);
