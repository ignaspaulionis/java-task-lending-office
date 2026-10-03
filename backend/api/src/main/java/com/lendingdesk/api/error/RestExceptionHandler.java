package com.lendingdesk.api.error;

import com.lendingdesk.core.error.ErrorCode;
import com.lendingdesk.core.error.LendingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Turns exceptions into RFC 9457 problem responses with a stable {@code code} property. */
@RestControllerAdvice
public class RestExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

  @ExceptionHandler(LendingException.class)
  public ResponseEntity<ProblemDetail> handleLending(LendingException e) {
    HttpStatus status =
        switch (e.code().kind()) {
          case NOT_FOUND -> HttpStatus.NOT_FOUND;
          case CONFLICT -> HttpStatus.CONFLICT;
          case INVALID -> HttpStatus.BAD_REQUEST;
        };
    return problem(status, e.code().name(), e.getMessage());
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HandlerMethodValidationException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class,
    MissingServletRequestPartException.class,
    MultipartException.class,
    HttpMessageNotReadableException.class
  })
  public ResponseEntity<ProblemDetail> handleInvalidRequest(Exception e) {
    return problem(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED.name(), "Invalid request");
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException e) {
    return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", "No such endpoint");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ProblemDetail> handleMethod(HttpRequestMethodNotSupportedException e) {
    return problem(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", e.getMessage());
  }

  @ExceptionHandler(UnsupportedOperationException.class)
  public ResponseEntity<ProblemDetail> handleNotImplemented(UnsupportedOperationException e) {
    return problem(HttpStatus.NOT_IMPLEMENTED, "NOT_IMPLEMENTED", e.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnexpected(Exception e) {
    log.error("Unexpected error", e);
    return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error");
  }

  private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String code, String detail) {
    ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
    body.setProperty("code", code);
    return ResponseEntity.status(status).body(body);
  }
}
