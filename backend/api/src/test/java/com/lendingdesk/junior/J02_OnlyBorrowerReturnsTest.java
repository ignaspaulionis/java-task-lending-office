package com.lendingdesk.junior;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import com.lendingdesk.support.Response;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** J02 - Only the borrower returns. See JuniorTasks.md. */
class J02_OnlyBorrowerReturnsTest extends ApiTest {

  @Test
  void theBorrowerCanReturnTheLoan() {
    long asta = employee("Asta Demo");
    long loan = borrowOk(device("Dell XPS 13"), asta);
    clock.advance(Duration.ofHours(3));

    Response returned = returnLoan(loan, asta).expectStatus(200);

    assertThat((String) returned.json("$.loan.returnedAt")).isEqualTo("2026-03-10T13:00:00Z");
  }

  @Test
  void anotherEmployeeCannotReturnTheLoan() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long laptop = device("Dell XPS 13");
    long loan = borrowOk(laptop, asta);

    returnLoan(loan, jonas).expectProblem(409, "NOT_LOAN_OWNER");

    assertThat(activeLoansOfDevice(laptop)).isEqualTo(1);
    assertThat(getDevice(laptop).longAt("$.loanedTo")).isEqualTo(asta);
  }

  @Test
  void aLoanCannotBeReturnedTwice() {
    long asta = employee("Asta Demo");
    long loan = borrowOk(device("Dell XPS 13"), asta);
    returnLoan(loan, asta).expectStatus(200);
    clock.advance(Duration.ofDays(2));

    returnLoan(loan, asta).expectProblem(409, "LOAN_ALREADY_RETURNED");

    Response loans = api.get("/api/loans?active=false").expectStatus(200);
    assertThat((String) loans.json("$[0].returnedAt")).isEqualTo("2026-03-10T10:00:00Z");
  }

  @Test
  void returningAnUnknownLoanFails() {
    long asta = employee("Asta Demo");

    returnLoan(999, asta).expectProblem(404, "LOAN_NOT_FOUND");
  }
}
