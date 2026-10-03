package com.lendingdesk.mid;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import com.lendingdesk.support.Response;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** M02 - Employee summary without N+1. See MidTasks.md. */
class M02_SummaryQueriesTest extends ApiTest {

  @Test
  void summaryShowsLoansWithDeviceNamesAndWaitlistPositions() {
    long asta = insertEmployee("Asta Demo");
    long busy = insertBusyEmployee(asta);

    Response summary = summary(busy).expectStatus(200);

    assertThat((List<String>) summary.json("$.loans[*].deviceName"))
        .containsExactly("Own laptop 0", "Own laptop 1", "Own laptop 2");
    assertThat((List<String>) summary.json("$.waitlist[*].deviceName"))
        .containsExactly("Shared 0", "Shared 1", "Shared 2", "Shared 3", "Shared 4");
    assertThat((List<Integer>) summary.json("$.waitlist[*].position"))
        .containsExactly(2, 2, 2, 2, 2);
  }

  @Test
  void theNumberOfQueriesDoesNotGrowWithLoansAndWaitlistEntries() {
    long quiet = insertEmployee("Quiet Demo");
    insertActiveLoan(insertDevice("Quiet laptop"), quiet, NOW.plus(Duration.ofDays(7)));
    long asta = insertEmployee("Asta Demo");
    long busy = insertBusyEmployee(asta);

    Measurement small = measure(() -> summary(quiet));
    Measurement large = measure(() -> summary(busy));

    small.response().expectStatus(200);
    large.response().expectStatus(200);
    assertThat(large.statements()).as("SQL statements for 3 loans + 5 waitlist entries")
        .isLessThanOrEqualTo(3);
    assertThat(large.statements()).as("SQL statements compared to 1 loan")
        .isEqualTo(small.statements());
  }

  /** An employee with 3 loans who is second in line for 5 devices that Asta holds. */
  private long insertBusyEmployee(long asta) {
    long busy = insertEmployee("Busy Demo");
    for (int i = 0; i < 3; i++) {
      insertActiveLoan(insertDevice("Own laptop " + i), busy, NOW.plus(Duration.ofDays(7)));
    }
    long jonas = insertEmployee("Jonas Demo");
    for (int i = 0; i < 5; i++) {
      long shared = insertDevice("Shared " + i);
      insertActiveLoan(shared, asta, NOW.plus(Duration.ofDays(7)));
      insertWaitlistEntry(shared, jonas, NOW.minus(Duration.ofHours(2)));
      insertWaitlistEntry(shared, busy, NOW.minus(Duration.ofHours(1)));
    }
    return busy;
  }
}
