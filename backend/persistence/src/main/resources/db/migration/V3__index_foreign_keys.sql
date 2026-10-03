-- PostgreSQL does not index foreign key columns automatically.
-- (Numbered V3 so it sits next to the V2 migration of task M01.)
CREATE INDEX ix_loans_device_id ON loans (device_id);
CREATE INDEX ix_loans_employee_id ON loans (employee_id);
CREATE INDEX ix_waitlist_entries_device_id ON waitlist_entries (device_id);
CREATE INDEX ix_waitlist_entries_employee_id ON waitlist_entries (employee_id);
