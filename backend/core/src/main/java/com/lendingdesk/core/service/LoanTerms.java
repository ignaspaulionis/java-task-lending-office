package com.lendingdesk.core.service;

import com.lendingdesk.core.domain.Loan;
import java.math.BigDecimal;
import java.time.Instant;

/** Calculates the state of a loan at a given moment. */
public class LoanTerms {

  public boolean isOverdue(Loan loan, Instant now) {
    return loan.isActive() && loan.getDueAt().isBefore(now);
  }

  /** Late fee owed on an active loan; late fees are not implemented yet. */
  public BigDecimal lateFee(Loan loan, Instant now) {
    return null;
  }
}
