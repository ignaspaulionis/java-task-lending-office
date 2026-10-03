package com.lendingdesk.core.service;

import com.lendingdesk.core.LendingRules;
import com.lendingdesk.core.domain.Employee;
import com.lendingdesk.core.domain.Loan;
import com.lendingdesk.core.error.ErrorCode;
import com.lendingdesk.core.error.LendingException;
import com.lendingdesk.core.port.EmployeeRepository;
import com.lendingdesk.core.port.LoanRepository;
import jakarta.transaction.Transactional;
import java.time.Clock;

/** Hands a borrowed device over to a colleague. */
public class LoanTransferService {

  private final LoanRepository loans;
  private final EmployeeRepository employees;
  private final Clock clock;

  public LoanTransferService(LoanRepository loans, EmployeeRepository employees, Clock clock) {
    this.loans = loans;
    this.employees = employees;
    this.clock = clock;
  }

  /**
   * Ends the current loan and lends the device to a colleague with the same due date.
   *
   * @return the colleague's new loan
   */
  public Loan transfer(Long loanId, Long fromEmployeeId, Long toEmployeeId) {
    Loan loan =
        loans.findById(loanId).orElseThrow(() -> new LendingException(ErrorCode.LOAN_NOT_FOUND));
    if (!loan.isActive()) {
      throw new LendingException(ErrorCode.LOAN_ALREADY_RETURNED);
    }
    if (!loan.getEmployee().getId().equals(fromEmployeeId)) {
      throw new LendingException(ErrorCode.NOT_LOAN_OWNER);
    }
    return moveLoan(loan, toEmployeeId);
  }

  @Transactional
  public Loan moveLoan(Loan loan, Long toEmployeeId) {
    loan.markReturned(clock.instant());
    loans.save(loan);

    Employee colleague =
        employees
            .findById(toEmployeeId)
            .orElseThrow(() -> new LendingException(ErrorCode.EMPLOYEE_NOT_FOUND));
    if (!colleague.isActive()) {
      throw new LendingException(ErrorCode.EMPLOYEE_INACTIVE);
    }
    if (loans.findActiveByEmployee(toEmployeeId).size() >= LendingRules.MAX_ACTIVE_LOANS) {
      throw new LendingException(ErrorCode.LOAN_LIMIT_REACHED);
    }
    return loans.save(new Loan(loan.getDevice(), colleague, clock.instant(), loan.getDueAt()));
  }
}
