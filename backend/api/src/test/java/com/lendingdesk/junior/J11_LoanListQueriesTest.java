package com.lendingdesk.junior;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** J11 - Loan list without N+1. See JuniorTasks.md. */
class J11_LoanListQueriesTest extends ApiTest {

  @Test
  void theLoanListIsOneQueryNoMatterHowManyLoans() {
    long asta = insertEmployee("Asta Demo");
    for (int i = 0; i < 10; i++) {
      insertActiveLoan(insertDevice("Laptop " + i), asta, NOW.plus(Duration.ofDays(7)));
    }

    Measurement all = measure(() -> api.get("/api/loans"));
    Measurement filtered = measure(() -> api.get("/api/loans?active=true&employeeId=" + asta));

    assertThat((List<String>) all.response().expectStatus(200).json("$[*].deviceName"))
        .hasSize(10)
        .contains("Laptop 0", "Laptop 9");
    assertThat(all.statements()).as("SQL statements for GET /api/loans").isEqualTo(1);
    assertThat((List<Object>) filtered.response().expectStatus(200).json("$")).hasSize(10);
    assertThat(filtered.statements()).as("SQL statements with filters").isEqualTo(1);
  }
}
