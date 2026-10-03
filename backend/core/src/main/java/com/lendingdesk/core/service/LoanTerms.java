package com.lendingdesk.core.service;

import com.lendingdesk.core.LendingRules;
import com.lendingdesk.core.domain.Loan;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Calculates the state of a loan at a given moment. A loan becomes overdue at the first local
 * midnight (Europe/Vilnius) after its due time; every started local day costs a late fee.
 */
public class LoanTerms {

  public boolean isOverdue(Loan loan, Instant now) {
    return daysOverdue(loan, now) > 0;
  }

  /** Late fee owed on an active loan, with two decimals; {@code null} for returned loans. */
  public BigDecimal lateFee(Loan loan, Instant now) {
    if (!loan.isActive()) {
      return null;
    }
    BigDecimal fee = LendingRules.LATE_FEE_PER_DAY.multiply(BigDecimal.valueOf(daysOverdue(loan, now)));
    return fee.min(LendingRules.LATE_FEE_CAP).setScale(2, RoundingMode.UNNECESSARY);
  }

  /** Active loans due before this instant are overdue at {@code now}: the start of today locally. */
  public Instant overdueCutoff(Instant now) {
    return localDate(now).atStartOfDay(LendingRules.LOCAL_ZONE).toInstant();
  }

  private long daysOverdue(Loan loan, Instant now) {
    if (!loan.isActive()) {
      return 0;
    }
    long days = ChronoUnit.DAYS.between(localDate(loan.getDueAt()), localDate(now));
    return Math.max(days, 0);
  }

  private static LocalDate localDate(Instant instant) {
    return LocalDate.ofInstant(instant, LendingRules.LOCAL_ZONE);
  }
}
