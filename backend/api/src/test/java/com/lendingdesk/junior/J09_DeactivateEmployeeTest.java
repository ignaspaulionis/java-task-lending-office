package com.lendingdesk.junior;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** J09 - Deactivating an employee. See JuniorTasks.md. */
class J09_DeactivateEmployeeTest extends ApiTest {

  @Test
  void anEmployeeWithActiveLoansCannotBeDeactivated() {
    long asta = employee("Asta Demo");
    borrowOk(device("Dell XPS 13"), asta);

    updateEmployee(asta, false).expectProblem(409, "EMPLOYEE_HAS_LOANS");

    assertThat(isActive(asta)).isTrue();
  }

  @Test
  void anEmployeeWithLoansCanStillBeRenamed() {
    long asta = employee("Asta Demo");
    borrowOk(device("Dell XPS 13"), asta);

    api.put(
            "/api/employees/" + asta,
            Map.of("name", "Asta Renamed", "email", "asta.demo@example.com", "active", true))
        .expectStatus(200);
  }

  @Test
  void anEmployeeCanBeDeactivatedOnceEverythingIsReturned() {
    long asta = employee("Asta Demo");
    long loan = borrowOk(device("Dell XPS 13"), asta);
    returnLoan(loan, asta).expectStatus(200);

    deactivate(asta);

    assertThat(isActive(asta)).isFalse();
  }

  @Test
  void deactivatingRemovesTheEmployeeFromEveryWaitlist() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long ruta = employee("Ruta Demo");
    long laptop = device("Dell XPS 13");
    long monitor = device("LG 27UL850");
    borrowOk(laptop, asta);
    borrowOk(monitor, asta);
    joinWaitlist(laptop, jonas).expectStatus(201);
    joinWaitlist(monitor, jonas).expectStatus(201);
    joinWaitlist(laptop, ruta).expectStatus(201);

    deactivate(jonas);

    assertThat((List<Object>) summary(jonas).json("$.waitlist")).isEmpty();
    assertThat(summary(ruta).longAt("$.waitlist[0].position")).isEqualTo(1);
  }

  @Test
  void anInactiveEmployeeCanBeReactivated() {
    long asta = employee("Asta Demo");
    deactivate(asta);

    updateEmployee(asta, true).expectStatus(200);

    assertThat(isActive(asta)).isTrue();
  }

  private boolean isActive(long employeeId) {
    List<Boolean> active =
        api.get("/api/employees").expectStatus(200).json("$[?(@.id == " + employeeId + ")].active");
    return active.getFirst();
  }
}
