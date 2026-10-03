package com.lendingdesk.junior;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import com.lendingdesk.support.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** J18 - New endpoint: loan history. See JuniorTasks.md. */
class J18_LoanHistoryTest extends ApiTest {

  @Test
  void listsReturnedLoansNewestFirst() {
    long asta = insertEmployee("Asta Demo");
    long jonas = insertEmployee("Jonas Demo");
    long older = returnedLoan(asta, "Dell XPS 13", "2026-02-01T10:00:00Z", "2026-02-10T15:00:00Z");
    long newer = returnedLoan(asta, "LG 27UL850", "2026-02-05T10:00:00Z", "2026-02-20T09:30:00Z");
    returnedLoan(jonas, "Pixel 9", "2026-02-03T10:00:00Z", "2026-02-04T10:00:00Z");
    insertActiveLoan(insertDevice("Still borrowed"), asta, NOW.plus(Duration.ofDays(5)));

    Response history = api.get("/api/employees/" + asta + "/loan-history").expectStatus(200);

    List<Map<String, Object>> entries = history.json("$");
    assertThat(entries).hasSize(2);
    assertThat(entries.getFirst())
        .containsOnlyKeys("loanId", "deviceId", "deviceName", "borrowedAt", "returnedAt")
        .containsEntry("loanId", (int) newer)
        .containsEntry("deviceName", "LG 27UL850")
        .containsEntry("borrowedAt", "2026-02-05T10:00:00Z")
        .containsEntry("returnedAt", "2026-02-20T09:30:00Z");
    assertThat(entries.get(1)).containsEntry("loanId", (int) older);
  }

  @Test
  void anEmployeeWithoutReturnedLoansHasAnEmptyHistory() {
    long asta = insertEmployee("Asta Demo");

    Response history = api.get("/api/employees/" + asta + "/loan-history").expectStatus(200);

    assertThat((List<Object>) history.json("$")).isEmpty();
  }

  @Test
  void anUnknownEmployeeIsNotFound() {
    api.get("/api/employees/999/loan-history").expectProblem(404, "EMPLOYEE_NOT_FOUND");
  }

  @Test
  void theHistoryIsLoadedWithoutOneQueryPerLoan() {
    long asta = insertEmployee("Asta Demo");
    for (int i = 0; i < 5; i++) {
      Instant borrowed = Instant.parse("2026-02-01T10:00:00Z").plus(Duration.ofDays(i));
      returnedLoan(asta, "Laptop " + i, borrowed.toString(), borrowed.plus(Duration.ofDays(2)).toString());
    }

    Measurement history = measure(() -> api.get("/api/employees/" + asta + "/loan-history"));

    assertThat((List<Object>) history.response().expectStatus(200).json("$")).hasSize(5);
    assertThat(history.statements()).as("SQL statements").isLessThanOrEqualTo(2);
  }

  private long returnedLoan(long employeeId, String deviceName, String borrowedAt, String returnedAt) {
    Instant borrowed = Instant.parse(borrowedAt);
    return insertLoan(
        insertDevice(deviceName),
        employeeId,
        borrowed,
        borrowed.plus(Duration.ofDays(14)),
        Instant.parse(returnedAt));
  }
}
