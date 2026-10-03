package com.lendingdesk.junior;

import static org.assertj.core.api.Assertions.assertThat;

import com.lendingdesk.support.ApiTest;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/** J05 - Loan limit, clean and cheap. See JuniorTasks.md. */
class J05_LoanLimitTest extends ApiTest {

  @Test
  void anEmployeeMayHaveThreeActiveLoansButNotFour() {
    long asta = employee("Asta Demo");
    for (int i = 0; i < 3; i++) {
      borrowOk(device("Laptop " + i), asta);
    }

    borrow(device("Laptop 4"), asta).expectProblem(409, "LOAN_LIMIT_REACHED");

    assertThat(activeLoansOfEmployee(asta)).isEqualTo(3);
  }

  @Test
  void returnedLoansDoNotCount() {
    long asta = employee("Asta Demo");
    long first = borrowOk(device("Laptop 1"), asta);
    borrowOk(device("Laptop 2"), asta);
    borrowOk(device("Laptop 3"), asta);
    returnLoan(first, asta).expectStatus(200);

    borrow(device("Laptop 4"), asta).expectStatus(201);
  }

  @Test
  void otherEmployeesLoansDoNotCount() {
    long asta = employee("Asta Demo");
    long jonas = employee("Jonas Demo");
    for (int i = 0; i < 3; i++) {
      borrowOk(device("Laptop " + i), jonas);
    }

    borrow(device("Laptop for Asta"), asta).expectStatus(201);
  }

  @Test
  void theLimitIsCheckedInTheDatabaseWithoutLoadingLoans() {
    long asta = insertEmployee("Asta Demo");
    insertActiveLoan(insertDevice("Asta laptop 1"), asta, NOW.plus(Duration.ofDays(3)));
    insertActiveLoan(insertDevice("Asta laptop 2"), asta, NOW.plus(Duration.ofDays(5)));
    long others = insertEmployee("Many Loans");
    for (int i = 0; i < 50; i++) {
      insertActiveLoan(insertDevice("Other laptop " + i), others, NOW.plus(Duration.ofDays(7)));
    }
    long laptop = insertDevice("Dell XPS 13");

    Measurement borrowing = measure(() -> borrow(laptop, asta));

    borrowing.response().expectStatus(201);
    assertThat(borrowing.loansLoaded())
        .as("loan entities loaded into memory while borrowing")
        .isZero();
  }
}
