package com.lendingdesk.junior;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import com.lendingdesk.support.Response;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** J16 - Transaction bug: transferring a loan. See JuniorTasks.md. */
class J16_TransferLoanTest extends ApiTest {

  @Test
  void aLoanCanBeHandedToAColleagueWithTheSameDueDate() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long laptop = device("Dell XPS 13");
    long loan = borrowOk(laptop, asta);

    Response transferred = transfer(loan, asta, jonas).expectStatus(201);

    assertThat(transferred.longAt("$.employeeId")).isEqualTo(jonas);
    assertThat((String) transferred.json("$.dueAt")).isEqualTo("2026-03-24T10:00:00Z");
    assertThat(getDevice(laptop).longAt("$.loanedTo")).isEqualTo(jonas);
    assertThat(activeLoansOfEmployee(asta)).isZero();
  }

  @Test
  void onlyTheBorrowerCanTransfer() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long loan = borrowOk(device("Dell XPS 13"), asta);

    transfer(loan, jonas, jonas).expectProblem(409, "NOT_LOAN_OWNER");
  }

  @Test
  void aFailedTransferToAColleagueAtTheLimitChangesNothing() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long laptop = device("Dell XPS 13");
    long loan = borrowOk(laptop, asta);
    for (int i = 0; i < 3; i++) {
      borrowOk(device("Monitor " + i), jonas);
    }

    transfer(loan, asta, jonas).expectProblem(409, "LOAN_LIMIT_REACHED");

    assertStillHeldBy(laptop, asta);
  }

  @Test
  void aFailedTransferToAnInactiveColleagueChangesNothing() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long laptop = device("Dell XPS 13");
    long loan = borrowOk(laptop, asta);
    deactivate(jonas);

    transfer(loan, asta, jonas).expectProblem(409, "EMPLOYEE_INACTIVE");

    assertStillHeldBy(laptop, asta);
  }

  @Test
  void aFailedTransferToAnUnknownColleagueChangesNothing() {
    long asta = employee("Asta Demo");
    long laptop = device("Dell XPS 13");
    long loan = borrowOk(laptop, asta);

    transfer(loan, asta, 999).expectProblem(404, "EMPLOYEE_NOT_FOUND");

    assertStillHeldBy(laptop, asta);
  }

  private void assertStillHeldBy(long deviceId, long employeeId) {
    assertThat(getDevice(deviceId).longAt("$.loanedTo")).as("holder of the device").isEqualTo(employeeId);
    assertThat(activeLoansOfEmployee(employeeId)).isEqualTo(1);
  }

  private Response transfer(long loanId, long fromEmployeeId, long toEmployeeId) {
    return api.post(
        "/api/loans/" + loanId + "/transfer",
        Map.of("fromEmployeeId", fromEmployeeId, "toEmployeeId", toEmployeeId));
  }
}
