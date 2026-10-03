package com.lendingdesk.core.model;

import java.util.List;

/**
 * Result of a CSV import.
 *
 * @param errors skipped rows, ordered by line number (the header is line 1)
 */
public record ImportReport(int imported, List<Error> errors) {

  public record Error(int line, ImportProblem reason) {}
}
