-- One employee per email address, ignoring case.
CREATE UNIQUE INDEX ux_employees_email_lower ON employees (lower(email));
