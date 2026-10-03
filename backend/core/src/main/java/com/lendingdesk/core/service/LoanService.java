package com.lendingdesk.core.service;

import com.lendingdesk.core.LendingRules;
import com.lendingdesk.core.domain.Device;
import com.lendingdesk.core.domain.DeviceStatus;
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

  /**
   * Lends a device to an employee for {@link LendingRules#LOAN_PERIOD}.
   *
   * <p>Locks the device row, then the employee row, so that parallel requests cannot lend the
   * same device twice or push an employee over the limit. Every method that takes several locks
   * takes them in the order loan, device, employee to avoid deadlocks.
   */
  @Transactional
  public Loan borrow(Long deviceId, Long employeeId) {
    Device device =
        devices
            .findByIdForUpdate(deviceId)
            .orElseThrow(() -> new LendingException(ErrorCode.DEVICE_NOT_FOUND));
    Employee employee =
        employees
            .findByIdForUpdate(employeeId)
            .orElseThrow(() -> new LendingException(ErrorCode.EMPLOYEE_NOT_FOUND));
    if (!employee.isActive()) {
      throw new LendingException(ErrorCode.EMPLOYEE_INACTIVE);
    }
    if (device.getStatus() != DeviceStatus.AVAILABLE) {
      throw new LendingException(ErrorCode.DEVICE_NOT_AVAILABLE);
    }
    if (loans.findActiveByDevice(deviceId).isPresent()) {
      throw new LendingException(ErrorCode.DEVICE_ALREADY_LOANED);
    }
    List<WaitlistEntry> queue = waitlist.findByDevice(deviceId);
    if (!queue.isEmpty() && !queue.getFirst().getEmployee().getId().equals(employeeId)) {
      throw new LendingException(ErrorCode.NOT_FIRST_IN_QUEUE);
    }
    if (hasReachedLoanLimit(employeeId)) {
      throw new LendingException(ErrorCode.LOAN_LIMIT_REACHED);
    }
    if (!queue.isEmpty()) {
      waitlist.delete(queue.getFirst());
    }
    return lend(device, employee);
  }

  /** Marks a loan as returned and hands the device to the next eligible employee in line. */
  @Transactional
  public ReturnResult returnLoan(Long loanId, Long employeeId) {
    Loan loan =
        loans
            .findByIdForUpdate(loanId)
            .orElseThrow(() -> new LendingException(ErrorCode.LOAN_NOT_FOUND));
    if (!loan.isActive()) {
      throw new LendingException(ErrorCode.LOAN_ALREADY_RETURNED);
    }
    if (!loan.getEmployee().getId().equals(employeeId)) {
      throw new LendingException(ErrorCode.NOT_LOAN_OWNER);
    }
    loan.markReturned(clock.instant());
    // Flush now: the next loan of this device must not be inserted while this one looks active.
    Loan returned = loans.saveAndFlush(loan);
    Device device = devices.findByIdForUpdate(loan.getDevice().getId()).orElseThrow();
    return new ReturnResult(returned, handOver(device));
  }

  /** Active loans whose due time has passed, most overdue first. */
  @Transactional
  public List<Loan> overdue() {
    return loans.findActiveDueBefore(clock.instant());
  }

  @Transactional
  public List<Loan> list(Long employeeId, Boolean active) {
    return loans.find(employeeId, active);
  }

  /**
   * Lends the device to the first eligible employee in its waitlist. Ineligible employees are
   * removed from the queue. A device that is not available is not handed over.
   *
   * @return the employee who got the device, or {@code null}
   */
  private Long handOver(Device device) {
    if (device.getStatus() != DeviceStatus.AVAILABLE) {
      return null;
    }
    for (WaitlistEntry entry : waitlist.findByDevice(device.getId())) {
      waitlist.delete(entry);
      Employee candidate = employees.findByIdForUpdate(entry.getEmployee().getId()).orElseThrow();
      if (candidate.isActive() && !hasReachedLoanLimit(candidate.getId())) {
        lend(device, candidate);
        return candidate.getId();
      }
    }
    return null;
  }

  private Loan lend(Device device, Employee employee) {
    Instant now = clock.instant();
    return loans.save(new Loan(device, employee, now, now.plus(LendingRules.LOAN_PERIOD)));
  }

  private boolean hasReachedLoanLimit(Long employeeId) {
    return loans.countActiveByEmployee(employeeId) >= LendingRules.MAX_ACTIVE_LOANS;
  }
}
