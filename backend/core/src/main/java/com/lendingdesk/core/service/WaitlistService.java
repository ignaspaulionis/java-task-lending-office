package com.lendingdesk.core.service;

import com.lendingdesk.core.domain.Device;
import com.lendingdesk.core.domain.DeviceStatus;
import com.lendingdesk.core.domain.Employee;
import com.lendingdesk.core.domain.Loan;
import com.lendingdesk.core.domain.WaitlistEntry;
import com.lendingdesk.core.error.ErrorCode;
import com.lendingdesk.core.error.LendingException;
import com.lendingdesk.core.model.WaitlistJoinResult;
import com.lendingdesk.core.port.DeviceRepository;
import com.lendingdesk.core.port.EmployeeRepository;
import com.lendingdesk.core.port.LoanRepository;
import com.lendingdesk.core.port.WaitlistRepository;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

public class WaitlistService {

  private final WaitlistRepository waitlist;
  private final DeviceRepository devices;
  private final EmployeeRepository employees;
  private final LoanRepository loans;
  private final LoanService loanService;
  private final Clock clock;

  public WaitlistService(
      WaitlistRepository waitlist,
      DeviceRepository devices,
      EmployeeRepository employees,
      LoanRepository loans,
      LoanService loanService,
      Clock clock) {
    this.waitlist = waitlist;
    this.devices = devices;
    this.employees = employees;
    this.loans = loans;
    this.loanService = loanService;
    this.clock = clock;
  }

  /**
   * Adds the employee to the end of the device's waitlist, or lends the device right away when
   * it is free and nobody is waiting for it.
   */
  @Transactional
  public WaitlistJoinResult join(Long deviceId, Long employeeId) {
    Device device =
        devices
            .findById(deviceId)
            .orElseThrow(() -> new LendingException(ErrorCode.DEVICE_NOT_FOUND));
    Employee employee =
        employees
            .findById(employeeId)
            .orElseThrow(() -> new LendingException(ErrorCode.EMPLOYEE_NOT_FOUND));
    Optional<Loan> activeLoan = loans.findActiveByDevice(deviceId);
    if (activeLoan.isPresent() && activeLoan.get().getEmployee().getId().equals(employeeId)) {
      throw new LendingException(ErrorCode.ALREADY_WAITLISTED);
    }
    List<WaitlistEntry> queue = waitlist.findByDevice(deviceId);
    if (queue.stream().anyMatch(entry -> entry.getEmployee().getId().equals(employeeId))) {
      throw new LendingException(ErrorCode.ALREADY_WAITLISTED);
    }
    if (activeLoan.isEmpty() && queue.isEmpty() && device.getStatus() == DeviceStatus.AVAILABLE) {
      return new WaitlistJoinResult(null, loanService.borrow(deviceId, employeeId));
    }
    waitlist.save(new WaitlistEntry(device, employee, clock.instant()));
    return new WaitlistJoinResult(queue.size() + 1, null);
  }

  /** Removes the employee from the device's waitlist. */
  @Transactional
  public void leave(Long deviceId, Long employeeId) {
    devices.findById(deviceId).orElseThrow(() -> new LendingException(ErrorCode.DEVICE_NOT_FOUND));
    WaitlistEntry entry =
        waitlist
            .findByDeviceAndEmployee(deviceId, employeeId)
            .orElseThrow(() -> new LendingException(ErrorCode.NOT_WAITLISTED));
    waitlist.delete(entry);
  }
}
