package com.lendingdesk.junior;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import com.lendingdesk.support.Response;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** J06 - Overdue list in the database. See JuniorTasks.md. */
class J06_OverdueQueryTest extends ApiTest {

  @Test
  void listsOnlyActiveOverdueLoansMostOverdueFirst() {
    long asta = insertEmployee("Asta Demo");
    long twoDaysLate =
        insertActiveLoan(insertDevice("Laptop B"), asta, NOW.minus(Duration.ofDays(2)));
    long fiveDaysLate =
        insertActiveLoan(insertDevice("Laptop A"), asta, NOW.minus(Duration.ofDays(5)));
    insertLoan(
        insertDevice("Returned laptop"),
        asta,
        NOW.minus(Duration.ofDays(30)),
        NOW.minus(Duration.ofDays(16)),
        NOW.minus(Duration.ofDays(10)));
    insertActiveLoan(insertDevice("Not due yet"), asta, NOW.plus(Duration.ofDays(3)));

    Response overdue = api.get("/api/loans/overdue").expectStatus(200);

    List<Number> ids = overdue.json("$[*].id");
    assertThat(ids.stream().map(Number::longValue)).containsExactly(fiveDaysLate, twoDaysLate);
  }

  @Test
  void theOverdueListIsOneQueryNoMatterHowManyLoansExist() {
    long asta = insertEmployee("Asta Demo");
    insertActiveLoan(insertDevice("Late laptop 1"), asta, NOW.minus(Duration.ofDays(4)));
    insertActiveLoan(insertDevice("Late laptop 2"), asta, NOW.minus(Duration.ofDays(2)));
    long others = insertEmployee("Many Loans");
    for (int i = 0; i < 30; i++) {
      insertActiveLoan(insertDevice("Laptop " + i), others, NOW.plus(Duration.ofDays(7)));
    }

    Measurement overdue = measure(() -> api.get("/api/loans/overdue"));

    overdue.response().expectStatus(200);
    assertThat((List<Object>) overdue.response().json("$")).hasSize(2);
    assertThat(overdue.statements()).as("SQL statements").isEqualTo(1);
    assertThat(overdue.loansLoaded()).as("loan entities loaded into memory").isEqualTo(2);
  }
}
