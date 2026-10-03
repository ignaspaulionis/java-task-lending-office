package com.lendingdesk.junior;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import com.lendingdesk.support.Response;
import java.util.List;
import org.junit.jupiter.api.Test;

/** J04 - Hand the device to the next in line. See JuniorTasks.md. */
class J04_HandOffOnReturnTest extends ApiTest {

  @Test
  void returningHandsTheDeviceToTheFirstInLine() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long ruta = employee("Ruta Demo");
    long laptop = device("Dell XPS 13");
    long loan = borrowOk(laptop, asta);
    joinWaitlist(laptop, jonas).expectStatus(201);
    joinWaitlist(laptop, ruta).expectStatus(201);

    Response returned = returnLoan(loan, asta).expectStatus(200);

    assertThat(returned.longAt("$.nextEmployeeId")).isEqualTo(jonas);
    assertThat(getDevice(laptop).longAt("$.loanedTo")).isEqualTo(jonas);
    assertThat((List<Object>) summary(jonas).json("$.waitlist")).isEmpty();
    assertThat((List<String>) summary(jonas).json("$.loans[*].deviceName"))
        .containsExactly("Dell XPS 13");
    assertThat(summary(ruta).longAt("$.waitlist[0].position")).isEqualTo(1);
  }

  @Test
  void inactiveEmployeesAreSkippedAndRemovedFromTheQueue() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long ruta = employee("Ruta Demo");
    long laptop = device("Dell XPS 13");
    long loan = borrowOk(laptop, asta);
    joinWaitlist(laptop, jonas).expectStatus(201);
    joinWaitlist(laptop, ruta).expectStatus(201);
    deactivate(jonas);

    Response returned = returnLoan(loan, asta).expectStatus(200);

    assertThat(returned.longAt("$.nextEmployeeId")).isEqualTo(ruta);
    assertThat(getDevice(laptop).longAt("$.loanedTo")).isEqualTo(ruta);
    assertThat((List<Object>) summary(jonas).json("$.waitlist")).isEmpty();
  }

  @Test
  void employeesAtTheirLoanLimitAreSkippedAndRemovedFromTheQueue() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long ruta = employee("Ruta Demo");
    long laptop = device("Dell XPS 13");
    long loan = borrowOk(laptop, asta);
    joinWaitlist(laptop, jonas).expectStatus(201);
    joinWaitlist(laptop, ruta).expectStatus(201);
    for (int i = 0; i < 3; i++) {
      borrowOk(device("Monitor " + i), jonas);
    }

    Response returned = returnLoan(loan, asta).expectStatus(200);

    assertThat(returned.longAt("$.nextEmployeeId")).isEqualTo(ruta);
    assertThat(activeLoansOfEmployee(jonas)).isEqualTo(3);
    assertThat((List<Object>) summary(jonas).json("$.waitlist")).isEmpty();
  }

  @Test
  void whenNobodyIsEligibleTheDeviceBecomesFree() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long laptop = device("Dell XPS 13");
    long loan = borrowOk(laptop, asta);
    joinWaitlist(laptop, jonas).expectStatus(201);
    deactivate(jonas);

    Response returned = returnLoan(loan, asta).expectStatus(200);

    assertThat((Object) returned.json("$.nextEmployeeId")).isNull();
    assertThat((Boolean) getDevice(laptop).json("$.available")).isTrue();
    assertThat((List<Object>) summary(jonas).json("$.waitlist")).isEmpty();
  }

  @Test
  void aDeviceInMaintenanceIsNotHandedOverAndTheQueueWaits() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long ruta = employee("Ruta Demo");
    long laptop = device("Dell XPS 13");
    long loan = borrowOk(laptop, asta);
    joinWaitlist(laptop, jonas).expectStatus(201);
    joinWaitlist(laptop, ruta).expectStatus(201);
    setStatus(laptop, "MAINTENANCE");

    Response returned = returnLoan(loan, asta).expectStatus(200);

    assertThat((Object) returned.json("$.nextEmployeeId")).isNull();
    assertThat((Object) getDevice(laptop).json("$.loanedTo")).isNull();
    assertThat(summary(jonas).longAt("$.waitlist[0].position")).isEqualTo(1);
    assertThat(summary(ruta).longAt("$.waitlist[0].position")).isEqualTo(2);
  }

  @Test
  void whileAQueueExistsOnlyTheFirstInLineMayBorrow() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long ruta = employee("Ruta Demo");
    long laptop = device("Dell XPS 13");
    long loan = borrowOk(laptop, asta);
    joinWaitlist(laptop, jonas).expectStatus(201);
    joinWaitlist(laptop, ruta).expectStatus(201);
    setStatus(laptop, "MAINTENANCE");
    returnLoan(loan, asta).expectStatus(200);
    setStatus(laptop, "AVAILABLE");

    borrow(laptop, ruta).expectProblem(409, "NOT_FIRST_IN_QUEUE");
    borrow(laptop, jonas).expectStatus(201);

    assertThat((List<Object>) summary(jonas).json("$.waitlist")).isEmpty();
    assertThat(summary(ruta).longAt("$.waitlist[0].position")).isEqualTo(1);
  }
}
