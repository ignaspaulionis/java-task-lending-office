package com.lendingdesk.junior;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import com.lendingdesk.support.Response;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * J15 - Bug hunt: cancelling a loan. See JuniorTasks.md.
 *
 * <p>These tests describe how cancelling works and they pass. The bug from the report is not
 * covered yet: reproduce it with a new test in this class, then fix it.
 */
class J15_CancelLoanTest extends ApiTest {

  @Test
  void theBorrowerCanCancelWithinFifteenMinutes() {
    long asta = employee("Asta Demo");
    long laptop = device("Dell XPS 13");
    long loan = borrowOk(laptop, asta);
    clock.advance(Duration.ofMinutes(10));

    cancel(loan, asta).expectStatus(204);

    assertThat((List<Object>) api.get("/api/loans").expectStatus(200).json("$")).isEmpty();
    assertThat((Boolean) getDevice(laptop).json("$.available")).isTrue();
  }

  @Test
  void aLoanCannotBeCancelledAfterFifteenMinutes() {
    long asta = employee("Asta Demo");
    long loan = borrowOk(device("Dell XPS 13"), asta);
    clock.advance(Duration.ofMinutes(16));

    cancel(loan, asta).expectProblem(409, "CANCEL_WINDOW_CLOSED");
  }

  @Test
  void onlyTheBorrowerCanCancel() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long loan = borrowOk(device("Dell XPS 13"), asta);

    cancel(loan, jonas).expectProblem(409, "NOT_LOAN_OWNER");
  }

  @Test
  void returnedAndUnknownLoansCannotBeCancelled() {
    long asta = employee("Asta Demo");
    long loan = borrowOk(device("Dell XPS 13"), asta);
    returnLoan(loan, asta).expectStatus(200);

    cancel(loan, asta).expectProblem(409, "LOAN_ALREADY_RETURNED");
    cancel(999, asta).expectProblem(404, "LOAN_NOT_FOUND");
  }

  /** Regression test for the bug report: ids above 127 are not cached Long instances. */
  @Test
  void theBorrowerCanCancelWhenTheirIdIsAbove127() {
    jdbc.execute("ALTER TABLE employees ALTER COLUMN id RESTART WITH 1000");
    long asta = employee("Asta Demo");
    long loan = borrowOk(device("Dell XPS 13"), asta);

    cancel(loan, asta).expectStatus(204);
  }

  private Response cancel(long loanId, long employeeId) {
    return api.post("/api/loans/" + loanId + "/cancel", Map.of("employeeId", employeeId));
  }
}
