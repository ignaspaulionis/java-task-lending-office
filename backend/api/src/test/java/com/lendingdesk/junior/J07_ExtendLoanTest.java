package com.lendingdesk.junior;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import com.lendingdesk.support.Response;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** J07 - Extend a loan. See JuniorTasks.md. */
class J07_ExtendLoanTest extends ApiTest {

  @Test
  void theBorrowerCanExtendALoanBySevenDays() {
    long asta = employee("Asta Demo");
    long loan = borrowOk(device("Dell XPS 13"), asta); // due 2026-03-24T10:00Z

    Response extended = extend(loan, asta).expectStatus(200);

    assertThat((String) extended.json("$.dueAt")).isEqualTo("2026-03-31T10:00:00Z");
  }

  @Test
  void aLoanCanBeExtendedOnlyOnce() {
    long asta = employee("Asta Demo");
    long loan = borrowOk(device("Dell XPS 13"), asta);
    extend(loan, asta).expectStatus(200);

    extend(loan, asta).expectProblem(409, "ALREADY_EXTENDED");

    assertThat(dueAt(loan)).isEqualTo("2026-03-31T10:00:00Z");
  }

  @Test
  void onlyTheBorrowerCanExtend() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long loan = borrowOk(device("Dell XPS 13"), asta);

    extend(loan, jonas).expectProblem(409, "NOT_LOAN_OWNER");

    assertThat(dueAt(loan)).isEqualTo("2026-03-24T10:00:00Z");
  }

  @Test
  void anOverdueLoanCannotBeExtended() {
    long asta = employee("Asta Demo");
    long loan = borrowOk(device("Dell XPS 13"), asta);
    clock.set(Instant.parse("2026-03-27T10:00:00Z"));

    extend(loan, asta).expectProblem(409, "LOAN_OVERDUE");
  }

  @Test
  void aLoanCannotBeExtendedWhileSomeoneIsWaiting() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long laptop = device("Dell XPS 13");
    long loan = borrowOk(laptop, asta);
    joinWaitlist(laptop, jonas).expectStatus(201);

    extend(loan, asta).expectProblem(409, "DEVICE_RESERVED");

    assertThat(dueAt(loan)).isEqualTo("2026-03-24T10:00:00Z");
  }

  @Test
  void returnedAndUnknownLoansCannotBeExtended() {
    long asta = employee("Asta Demo");
    long loan = borrowOk(device("Dell XPS 13"), asta);
    returnLoan(loan, asta).expectStatus(200);

    extend(loan, asta).expectProblem(409, "LOAN_ALREADY_RETURNED");
    extend(999, asta).expectProblem(404, "LOAN_NOT_FOUND");
  }

  private Response extend(long loanId, long employeeId) {
    return api.post("/api/loans/" + loanId + "/extend", Map.of("employeeId", employeeId));
  }

  private String dueAt(long loanId) {
    List<String> dueAt =
        api.get("/api/loans").expectStatus(200).json("$[?(@.id == " + loanId + ")].dueAt");
    return dueAt.getFirst();
  }
}
