package com.lendingdesk.core.service;

import com.lendingdesk.core.domain.Device;
import com.lendingdesk.core.domain.Employee;
import com.lendingdesk.core.domain.WaitlistEntry;
import com.lendingdesk.core.error.ErrorCode;
import com.lendingdesk.core.error.LendingException;
import com.lendingdesk.core.model.WaitlistJoinResult;
import com.lendingdesk.core.port.DeviceRepository;
import com.lendingdesk.core.port.EmployeeRepository;
import com.lendingdesk.core.port.WaitlistRepository;
import jakarta.transaction.Transactional;
import java.time.Clock;

public class WaitlistService {

  private final WaitlistRepository waitlist;
  private final DeviceRepository devices;
  private final EmployeeRepository employees;
  private final Clock clock;

  public WaitlistService(
      WaitlistRepository waitlist,
      DeviceRepository devices,
      EmployeeRepository employees,
      Clock clock) {
    this.waitlist = waitlist;
    this.devices = devices;
    this.employees = employees;
    this.clock = clock;
  }

  /** Adds the employee to the end of the device's waitlist. */
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
    waitlist.save(new WaitlistEntry(device, employee, clock.instant()));
    int position = waitlist.findByDevice(deviceId).size();
    return new WaitlistJoinResult(position, null);
  }

  /** Removes the employee from the device's waitlist. */
  @Transactional
  public void leave(Long deviceId, Long employeeId) {
    devices.findById(deviceId).orElseThrow(() -> new LendingException(ErrorCode.DEVICE_NOT_FOUND));
    waitlist.findByDeviceAndEmployee(deviceId, employeeId).ifPresent(waitlist::delete);
  }
}
