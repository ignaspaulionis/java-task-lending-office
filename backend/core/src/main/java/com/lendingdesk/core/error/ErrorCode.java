package com.lendingdesk.core.error;

/** Stable error codes returned to API clients. */
public enum ErrorCode {
  EMPLOYEE_NOT_FOUND(Kind.NOT_FOUND),
  DEVICE_NOT_FOUND(Kind.NOT_FOUND),
  LOAN_NOT_FOUND(Kind.NOT_FOUND),
  DEVICE_ALREADY_LOANED(Kind.CONFLICT),
  DEVICE_NOT_AVAILABLE(Kind.CONFLICT),
  EMPLOYEE_INACTIVE(Kind.CONFLICT),
  LOAN_LIMIT_REACHED(Kind.CONFLICT),
  NOT_LOAN_OWNER(Kind.CONFLICT),
  LOAN_ALREADY_RETURNED(Kind.CONFLICT),
  ALREADY_WAITLISTED(Kind.CONFLICT),
  NOT_WAITLISTED(Kind.CONFLICT),
  NOT_FIRST_IN_QUEUE(Kind.CONFLICT),
  DUPLICATE_INVENTORY_TAG(Kind.CONFLICT),
  ALREADY_EXTENDED(Kind.CONFLICT),
  LOAN_OVERDUE(Kind.CONFLICT),
  DEVICE_RESERVED(Kind.CONFLICT),
  EMPLOYEE_HAS_LOANS(Kind.CONFLICT),
  DEVICE_ON_LOAN(Kind.CONFLICT),
  DUPLICATE_EMAIL(Kind.CONFLICT),
  VALIDATION_FAILED(Kind.INVALID);

  /** How the API layer should classify the error. */
  public enum Kind {
    NOT_FOUND,
    CONFLICT,
    INVALID
  }

  private final Kind kind;

  ErrorCode(Kind kind) {
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }
}
