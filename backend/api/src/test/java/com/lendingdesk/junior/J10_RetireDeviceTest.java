package com.lendingdesk.junior;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** J10 - Retiring a device. See JuniorTasks.md. */
class J10_RetireDeviceTest extends ApiTest {

  @Test
  void aDeviceOnLoanCannotBeRetired() {
    long laptop = device("Dell XPS 13");
    borrowOk(laptop, employee("Asta Demo"));

    api.put("/api/devices/" + laptop, Map.of("name", "Dell XPS 13", "status", "RETIRED"))
        .expectProblem(409, "DEVICE_ON_LOAN");

    assertThat((String) getDevice(laptop).json("$.status")).isEqualTo("AVAILABLE");
  }

  @Test
  void aDeviceOnLoanCanStillGoToMaintenance() {
    long laptop = device("Dell XPS 13");
    borrowOk(laptop, employee("Asta Demo"));

    api.put("/api/devices/" + laptop, Map.of("name", "Dell XPS 13", "status", "MAINTENANCE"))
        .expectStatus(200);
  }

  @Test
  void retiringAFreeDeviceClearsItsWaitlist() {
    long jonas = employee("Jonas Demo");
    long ruta = employee("Ruta Demo");
    long laptop = device("Dell XPS 13");
    insertWaitlistEntry(laptop, jonas, NOW.minusSeconds(60));
    insertWaitlistEntry(laptop, ruta, NOW);

    api.put("/api/devices/" + laptop, Map.of("name", "Dell XPS 13", "status", "RETIRED"))
        .expectStatus(200);

    assertThat((List<Object>) summary(jonas).json("$.waitlist")).isEmpty();
    assertThat((List<Object>) summary(ruta).json("$.waitlist")).isEmpty();
  }
}
