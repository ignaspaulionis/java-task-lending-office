package com.lendingdesk.junior;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import com.lendingdesk.support.Response;
import java.util.List;
import org.junit.jupiter.api.Test;

/** J03 - Waitlist. See JuniorTasks.md. */
class J03_WaitlistTest extends ApiTest {

  @Test
  void employeesJoinTheWaitlistOfALoanedDeviceInOrder() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long ruta = employee("Ruta Demo");
    long laptop = device("Dell XPS 13");
    borrowOk(laptop, asta);

    Response first = joinWaitlist(laptop, jonas).expectStatus(201);
    Response second = joinWaitlist(laptop, ruta).expectStatus(201);

    assertThat(first.longAt("$.position")).isEqualTo(1);
    assertThat((Object) first.json("$.loan")).isNull();
    assertThat(second.longAt("$.position")).isEqualTo(2);
  }

  @Test
  void theSameEmployeeCannotJoinTheSameWaitlistTwice() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long laptop = device("Dell XPS 13");
    borrowOk(laptop, asta);
    joinWaitlist(laptop, jonas).expectStatus(201);

    joinWaitlist(laptop, jonas).expectProblem(409, "ALREADY_WAITLISTED");

    assertThat(summary(jonas).longAt("$.waitlist.length()")).isEqualTo(1);
  }

  @Test
  void theHolderCannotJoinTheWaitlistOfTheirOwnDevice() {
    long asta = employee("Asta Demo");
    long laptop = device("Dell XPS 13");
    borrowOk(laptop, asta);

    joinWaitlist(laptop, asta).expectProblem(409, "ALREADY_WAITLISTED");

    assertThat((List<Object>) summary(asta).json("$.waitlist")).isEmpty();
  }

  @Test
  void joiningTheWaitlistOfAFreeDeviceLoansItImmediately() {
    long asta = employee("Asta Demo");
    long laptop = device("Dell XPS 13");

    Response joined = joinWaitlist(laptop, asta).expectStatus(201);

    assertThat((Object) joined.json("$.position")).isNull();
    assertThat(joined.longAt("$.loan.employeeId")).isEqualTo(asta);
    assertThat(getDevice(laptop).longAt("$.loanedTo")).isEqualTo(asta);
    assertThat((List<Object>) summary(asta).json("$.waitlist")).isEmpty();
  }

  @Test
  void leavingAWaitlistYouAreNotOnFails() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long laptop = device("Dell XPS 13");
    borrowOk(laptop, asta);

    leaveWaitlist(laptop, jonas).expectProblem(409, "NOT_WAITLISTED");
  }

  @Test
  void leavingKeepsEveryoneElseInOrder() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    long ruta = employee("Ruta Demo");
    long tomas = employee("Tomas Demo");
    long laptop = device("Dell XPS 13");
    borrowOk(laptop, asta);
    joinWaitlist(laptop, jonas).expectStatus(201);
    joinWaitlist(laptop, ruta).expectStatus(201);
    joinWaitlist(laptop, tomas).expectStatus(201);

    leaveWaitlist(laptop, ruta).expectStatus(204);

    assertThat(summary(jonas).longAt("$.waitlist[0].position")).isEqualTo(1);
    assertThat(summary(tomas).longAt("$.waitlist[0].position")).isEqualTo(2);
  }
}
