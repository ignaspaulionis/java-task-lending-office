package com.lendingdesk.junior;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import com.lendingdesk.support.Response;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

/** J01 - No double loans. See JuniorTasks.md. */
class J01_NoDoubleLoanTest extends ApiTest {

  @Test
  void borrowingAFreeDeviceCreatesALoanDueIn14Days() {
    long asta = employee("Asta Demo");
    long laptop = device("Dell XPS 13");

    Response loan = borrow(laptop, asta).expectStatus(201);

    assertThat((String) loan.json("$.dueAt")).isEqualTo("2026-03-24T10:00:00Z");
  }

  @Test
  void aDeviceOnLoanCannotBeBorrowedAgain() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long laptop = device("Dell XPS 13");
    long loan = borrowOk(laptop, asta);

    borrow(laptop, jonas).expectProblem(409, "DEVICE_ALREADY_LOANED");

    assertThat(activeLoansOfDevice(laptop)).isEqualTo(1);
    Response active = api.get("/api/loans?active=true").expectStatus(200);
    assertThat(((List<Number>) active.json("$[*].id")).getFirst().longValue()).isEqualTo(loan);
    assertThat(getDevice(laptop).longAt("$.loanedTo")).isEqualTo(asta);
  }

  @Test
  void theSameEmployeeCannotBorrowTheSameDeviceTwice() {
    long asta = employee("Asta Demo");
    long laptop = device("Dell XPS 13");
    borrowOk(laptop, asta);

    borrow(laptop, asta).expectProblem(409, "DEVICE_ALREADY_LOANED");
  }

  @Test
  void aReturnedDeviceCanBeBorrowedAgain() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long laptop = device("Dell XPS 13");
    long loan = borrowOk(laptop, asta);
    clock.advance(Duration.ofDays(1));
    returnLoan(loan, asta).expectStatus(200);

    Response second = borrow(laptop, jonas).expectStatus(201);

    assertThat(second.longAt("$.employeeId")).isEqualTo(jonas);
  }

  @ParameterizedTest
  @ValueSource(strings = {"MAINTENANCE", "RETIRED"})
  void aDeviceThatIsNotAvailableCannotBeBorrowed(String status) {
    long asta = employee("Asta Demo");
    long laptop = device("Dell XPS 13");
    api.put("/api/devices/" + laptop, Map.of("name", "Dell XPS 13", "status", status))
        .expectStatus(200);

    borrow(laptop, asta).expectProblem(409, "DEVICE_NOT_AVAILABLE");

    assertThat(activeLoansOfDevice(laptop)).isZero();
  }

  @Test
  void anInactiveEmployeeCannotBorrow() {
    long asta = employee("Asta Demo");
    long laptop = device("Dell XPS 13");
    deactivate(asta);

    borrow(laptop, asta).expectProblem(409, "EMPLOYEE_INACTIVE");

    assertThat(activeLoansOfDevice(laptop)).isZero();
  }
}
