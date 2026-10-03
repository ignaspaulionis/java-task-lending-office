package com.lendingdesk.core.service;

import com.lendingdesk.core.LendingRules;
import com.lendingdesk.core.domain.Loan;
import com.lendingdesk.core.error.ErrorCode;
import com.lendingdesk.core.error.LendingException;
import com.lendingdesk.core.port.LoanRepository;
import jakarta.transaction.Transactional;
import java.time.Clock;

/** Lets an employee undo a loan they created by mistake. */
public class LoanCancellationService {

  private final LoanRepository loans;
  private final Clock clock;

  public LoanCancellationService(LoanRepository loans, Clock clock) {
    this.loans = loans;
    this.clock = clock;
  }

  /**
   * Deletes a loan, as if it never happened. Only the borrower can cancel, and only within
   * {@link LendingRules#CANCEL_WINDOW} of borrowing.
   */
  @Transactional
  public void cancel(Long loanId, Long employeeId) {
    Loan loan =
        loans.findById(loanId).orElseThrow(() -> new LendingException(ErrorCode.LOAN_NOT_FOUND));
    if (!loan.isActive()) {
      throw new LendingException(ErrorCode.LOAN_ALREADY_RETURNED);
    }
    // Long is an object: == compares references and only works for cached values (-128..127).
    if (!loan.getEmployee().getId().equals(employeeId)) {
      throw new LendingException(ErrorCode.NOT_LOAN_OWNER);
    }
    if (clock.instant().isAfter(loan.getBorrowedAt().plus(LendingRules.CANCEL_WINDOW))) {
      throw new LendingException(ErrorCode.CANCEL_WINDOW_CLOSED);
    }
    loans.delete(loan);
  }
}
