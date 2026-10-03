package com.lendingdesk.mid;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import com.lendingdesk.support.Response;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** M07 - Top devices report. See MidTasks.md. */
class M07_TopDevicesReportTest extends ApiTest {

  private static final String MARCH = "/api/reports/top-devices?from=2026-03-01&to=2026-04-01";

  @Test
  void listsTheFiveMostBorrowedDevicesTiesByName() {
    long asta = insertEmployee("Asta Demo");
    long alpha = devicesBorrowedInMarch(asta, "Alpha", 5);
    long charlie = devicesBorrowedInMarch(asta, "Charlie", 4);
    long bravo = devicesBorrowedInMarch(asta, "Bravo", 4);
    long delta = devicesBorrowedInMarch(asta, "Delta", 3);
    long echo = devicesBorrowedInMarch(asta, "Echo", 2);
    devicesBorrowedInMarch(asta, "Foxtrot", 1);
    long golf = insertDevice("Golf");
    for (int i = 0; i < 6; i++) {
      Instant february = Instant.parse("2026-02-10T10:00:00Z").plus(Duration.ofDays(i));
      insertLoan(golf, asta, february, february.plus(Duration.ofDays(14)), february);
    }

    Response report = api.get(MARCH).expectStatus(200);

    assertThat((List<Map<String, Object>>) report.json("$"))
        .containsExactly(
            Map.of("deviceId", (int) alpha, "deviceName", "Alpha", "loanCount", 5),
            Map.of("deviceId", (int) bravo, "deviceName", "Bravo", "loanCount", 4),
            Map.of("deviceId", (int) charlie, "deviceName", "Charlie", "loanCount", 4),
            Map.of("deviceId", (int) delta, "deviceName", "Delta", "loanCount", 3),
            Map.of("deviceId", (int) echo, "deviceName", "Echo", "loanCount", 2));
  }

  @Test
  void periodBoundariesAreLocalDatesInVilnius() {
    long asta = insertEmployee("Asta Demo");
    long hotel = insertDevice("Hotel");
    Instant firstMinutesOfMarch = Instant.parse("2026-02-28T22:30:00Z"); // 00:30 on 1 March
    Instant firstMinutesOfApril = Instant.parse("2026-03-31T21:30:00Z"); // 00:30 on 1 April
    insertLoan(hotel, asta, firstMinutesOfMarch, firstMinutesOfMarch, firstMinutesOfMarch);
    insertLoan(hotel, asta, firstMinutesOfApril, firstMinutesOfApril, firstMinutesOfApril);

    Response report = api.get(MARCH).expectStatus(200);

    assertThat(report.longAt("$[0].loanCount")).isEqualTo(1);
  }

  @Test
  void aPeriodWithoutLoansGivesAnEmptyList() {
    Response report = api.get(MARCH).expectStatus(200);

    assertThat((List<Object>) report.json("$")).isEmpty();
  }

  @Test
  void fromAfterToIsRejected() {
    api.get("/api/reports/top-devices?from=2026-04-01&to=2026-03-01")
        .expectProblem(400, "VALIDATION_FAILED");
  }

  @Test
  void theReportIsASingleQuery() {
    long asta = insertEmployee("Asta Demo");
    for (int i = 0; i < 8; i++) {
      devicesBorrowedInMarch(asta, "Device " + i, i + 1);
    }

    Measurement report = measure(() -> api.get(MARCH));

    report.response().expectStatus(200);
    assertThat(report.statements()).as("SQL statements").isEqualTo(1);
  }

  /** Inserts a device with {@code count} returned loans borrowed in March. */
  private long devicesBorrowedInMarch(long employeeId, String name, int count) {
    long device = insertDevice(name);
    for (int i = 0; i < count; i++) {
      Instant borrowed = Instant.parse("2026-03-02T10:00:00Z").plus(Duration.ofDays(i));
      insertLoan(device, employeeId, borrowed, borrowed.plus(Duration.ofDays(14)), borrowed);
    }
    return device;
  }
}
