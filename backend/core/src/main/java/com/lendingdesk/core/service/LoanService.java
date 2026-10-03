package com.lendingdesk.core.service;

import com.lendingdesk.core.LendingRules;
import com.lendingdesk.core.domain.Device;
import com.lendingdesk.core.domain.Employee;
import com.lendingdesk.core.domain.Loan;
import com.lendingdesk.core.domain.WaitlistEntry;
import com.lendingdesk.core.error.ErrorCode;
import com.lendingdesk.core.error.LendingException;
import com.lendingdesk.core.model.ReturnResult;
import com.lendingdesk.core.port.DeviceRepository;
import com.lendingdesk.core.port.EmployeeRepository;
import com.lendingdesk.core.port.LoanRepository;
import com.lendingdesk.core.port.WaitlistRepository;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

public class LoanService {

  private final LoanRepository loans;
  private final DeviceRepository devices;
  private final EmployeeRepository employees;
  private final WaitlistRepository waitlist;
  private final LoanTerms terms;
  private final Clock clock;

  public LoanService(
      LoanRepository loans,
      DeviceRepository devices,
      EmployeeRepository employees,
      WaitlistRepository waitlist,
      LoanTerms terms,
      Clock clock) {
    this.loans = loans;
    this.devices = devices;
    this.employees = employees;
    this.waitlist = waitlist;
    this.terms = terms;
    this.clock = clock;
  }

  /** Lends a device to an employee for {@link LendingRules#LOAN_PERIOD}. */
  @Transactional
  public Loan borrow(Long deviceId, Long employeeId) {
    Employee employee =
        employees
            .findById(employeeId)
            .orElseThrow(() -> new LendingException(ErrorCode.EMPLOYEE_NOT_FOUND));
    Device device =
        devices
            .findById(deviceId)
            .orElseThrow(() -> new LendingException(ErrorCode.DEVICE_NOT_FOUND));
    if (!canBorrowMore(employeeId)) {
      throw new LendingException(ErrorCode.LOAN_LIMIT_REACHED);
    }
    Instant now = clock.instant();
    return loans.save(new Loan(device, employee, now, now.plus(LendingRules.LOAN_PERIOD)));
  }

  /** Marks a loan as returned and reports who is next in the device's waitlist. */
  @Transactional
  public ReturnResult returnLoan(Long loanId, Long employeeId) {
    Loan loan =
        loans.findById(loanId).orElseThrow(() -> new LendingException(ErrorCode.LOAN_NOT_FOUND));
    loan.markReturned(clock.instant());
    List<WaitlistEntry> queue = waitlist.findByDevice(loan.getDevice().getId());
    Long next = queue.isEmpty() ? null : queue.get(0).getEmployee().getId();
    return new ReturnResult(loans.save(loan), next);
  }

  /** Active loans whose due time has passed. */
  @Transactional
  public List<Loan> overdue() {
    Instant now = clock.instant();
    return loans.findAll().stream().filter(loan -> terms.isOverdue(loan, now)).toList();
  }

  @Transactional
  public List<Loan> list(Long employeeId, Boolean active) {
    return loans.find(employeeId, active);
  }

  private boolean canBorrowMore(Long employeeId) {
    int active = 0;
    for (Loan loan : loans.findAll()) {
      if (loan.isActive() && loan.getEmployee().getId().equals(employeeId)) {
        active++;
      }
    }
    return active <= LendingRules.MAX_ACTIVE_LOANS;
  }
}
