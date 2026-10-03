package com.lendingdesk.mid;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import com.lendingdesk.support.Response;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * M05 - Late fees. See MidTasks.md.
 *
 * <p>Vilnius is UTC+2 until 2026-03-29 and UTC+3 after it.
 */
class M05_LateFeeTest extends ApiTest {

  @Test
  void aNewLoanHasNoFee() {
    Response loan = borrow(device("Dell XPS 13"), employee("Asta Demo")).expectStatus(201);

    assertThat((String) loan.json("$.lateFee")).isEqualTo("0.00");
  }

  @Test
  void aLoanIsNotOverdueDuringItsLocalDueDay() {
    // due 2026-03-24T10:00Z = 12:00 in Vilnius
    borrowOk(device("Dell XPS 13"), employee("Asta Demo"));

    clock.set(Instant.parse("2026-03-24T21:59:00Z")); // 23:59 in Vilnius

    assertLoan(false, "0.00");
    assertThat((List<Object>) api.get("/api/loans/overdue").expectStatus(200).json("$")).isEmpty();
  }

  @Test
  void oneMinuteAfterLocalMidnightTheLoanIsOneDayLate() {
    borrowOk(device("Dell XPS 13"), employee("Asta Demo"));

    clock.set(Instant.parse("2026-03-24T22:01:00Z")); // 00:01 on 25 March in Vilnius

    assertLoan(true, "0.50");
    assertThat((List<Object>) api.get("/api/loans/overdue").expectStatus(200).json("$"))
        .hasSize(1);
  }

  @Test
  void eachStartedDayAddsFiftyCents() {
    borrowOk(device("Dell XPS 13"), employee("Asta Demo"));

    clock.set(Instant.parse("2026-03-27T08:00:00Z")); // 27 March in Vilnius, 3 days late

    assertLoan(true, "1.50");
  }

  @Test
  void theFeeIsCappedAtTwentyEuros() {
    borrowOk(device("Dell XPS 13"), employee("Asta Demo"));

    clock.set(Instant.parse("2026-06-01T08:00:00Z"));

    assertLoan(true, "20.00");
  }

  @Test
  void dueDaysAreCalendarDaysInVilnius() {
    clock.set(Instant.parse("2026-03-10T23:30:00Z")); // 01:30 on 11 March in Vilnius
    borrowOk(device("Dell XPS 13"), employee("Asta Demo")); // due 01:30 on 25 March locally

    clock.set(Instant.parse("2026-03-25T21:30:00Z")); // 23:30 on 25 March in Vilnius
    assertLoan(false, "0.00");

    clock.set(Instant.parse("2026-03-25T22:30:00Z")); // 00:30 on 26 March in Vilnius
    assertLoan(true, "0.50");
  }

  private void assertLoan(boolean overdue, String lateFee) {
    Response loans = api.get("/api/loans?active=true").expectStatus(200);
    assertThat((Boolean) loans.json("$[0].overdue")).as("overdue, body: %s", loans.body())
        .isEqualTo(overdue);
    assertThat((String) loans.json("$[0].lateFee")).as("lateFee, body: %s", loans.body())
        .isEqualTo(lateFee);
  }
}
