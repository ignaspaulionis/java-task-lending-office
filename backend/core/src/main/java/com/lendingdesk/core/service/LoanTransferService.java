package com.lendingdesk.core.service;

import com.lendingdesk.core.LendingRules;
import com.lendingdesk.core.domain.Employee;
import com.lendingdesk.core.domain.Loan;
import com.lendingdesk.core.error.ErrorCode;
import com.lendingdesk.core.error.LendingException;
import com.lendingdesk.core.port.DeviceRepository;
import com.lendingdesk.core.port.EmployeeRepository;
import com.lendingdesk.core.port.LoanRepository;
import jakarta.transaction.Transactional;
import java.time.Clock;

/** Hands a borrowed device over to a colleague. */
public class LoanTransferService {

  private final LoanRepository loans;
  private final DeviceRepository devices;
  private final EmployeeRepository employees;
  private final Clock clock;

  public LoanTransferService(
      LoanRepository loans, DeviceRepository devices, EmployeeRepository employees, Clock clock) {
    this.loans = loans;
    this.devices = devices;
    this.employees = employees;
    this.clock = clock;
  }

  /**
   * Ends the current loan and lends the device to a colleague with the same due date.
   *
   * <p>The whole method is one transaction. (Before, {@code transfer} called a {@code
   * @Transactional} method of the same class; such self-calls bypass the Spring proxy, so no
   * transaction was started and each save was committed on its own.) Locks are taken in the
   * order loan, device, employee, like in {@link LoanService}.
   *
   * @return the colleague's new loan
   */
  @Transactional
  public Loan transfer(Long loanId, Long fromEmployeeId, Long toEmployeeId) {
    Loan loan =
        loans
            .findByIdForUpdate(loanId)
            .orElseThrow(() -> new LendingException(ErrorCode.LOAN_NOT_FOUND));
    if (!loan.isActive()) {
      throw new LendingException(ErrorCode.LOAN_ALREADY_RETURNED);
    }
    if (!loan.getEmployee().getId().equals(fromEmployeeId)) {
      throw new LendingException(ErrorCode.NOT_LOAN_OWNER);
    }
    devices.findByIdForUpdate(loan.getDevice().getId()).orElseThrow();
    Employee colleague =
        employees
            .findByIdForUpdate(toEmployeeId)
            .orElseThrow(() -> new LendingException(ErrorCode.EMPLOYEE_NOT_FOUND));
    if (!colleague.isActive()) {
      throw new LendingException(ErrorCode.EMPLOYEE_INACTIVE);
    }
    if (loans.countActiveByEmployee(toEmployeeId) >= LendingRules.MAX_ACTIVE_LOANS) {
      throw new LendingException(ErrorCode.LOAN_LIMIT_REACHED);
    }
    loan.markReturned(clock.instant());
    // Flush now: the new loan of this device must not be inserted while this one looks active.
    loans.saveAndFlush(loan);
    return loans.save(new Loan(loan.getDevice(), colleague, clock.instant(), loan.getDueAt()));
  }
}
