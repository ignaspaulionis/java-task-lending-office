package com.lendingdesk.core.error;

/** A business rule violation, reported to clients with its {@link ErrorCode}. */
public class LendingException extends RuntimeException {

  private final ErrorCode code;

  public LendingException(ErrorCode code) {
    this(code, code.name());
  }

  public LendingException(ErrorCode code, String message) {
    super(message);
    this.code = code;
  }

  public ErrorCode code() {
    return code;
  }
}
